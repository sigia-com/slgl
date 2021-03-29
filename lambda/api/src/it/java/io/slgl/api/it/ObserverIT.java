package io.slgl.api.it;

import io.slgl.api.it.data.PgpTestKeys;
import io.slgl.api.it.properties.Props;
import io.slgl.api.it.user.StateStorage;
import io.slgl.api.it.user.User;
import io.slgl.api.it.utils.ObserverFile;
import io.slgl.api.it.utils.ObserverFile.DataUploadedToS3;
import io.slgl.api.it.utils.PgpDecrypter;
import io.slgl.api.observer.service.PgpEncrypter;
import io.slgl.api.utils.ErrorCode;
import io.slgl.api.utils.json.UncheckedObjectMapper;
import io.slgl.client.camouflage.Camouflage;
import io.slgl.client.error.ErrorResponse;
import io.slgl.client.node.*;
import io.slgl.client.node.NodeRequest.Builder;
import io.slgl.client.storage.S3Storage;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.it.data.DocumentMother.*;
import static io.slgl.api.it.data.PdfMother.createPdf;
import static io.slgl.api.it.data.PermissionMother.allowAllForEveryone;
import static io.slgl.api.it.data.PgpTestKeys.*;
import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;
import static java.util.Objects.requireNonNull;

public class ObserverIT extends AbstractApiTest {

    public static final String OBSERVER_STORAGE_S3_BUCKET = Props.getSlglProperties().getObserverStorageS3Bucket();
    public static final String OBSERVER_STORAGE_S3_BUCKET_REGION = Props.getSlglProperties().getObserverStorageS3Region();
    public static final String OBSERVER_DEAD_LETTER_BUCKET = Props.getSlglProperties().getObserverDeadLetterBucket();

    private Camouflage camouflage = Camouflage.builder()
            .anchor("#observersC", "#observers")
            .anchor("#auditorsC", "#auditors")
            .anchor("#documentsC", "#documents")
            .build();

    @Test
    public void shouldNotDefineDefaultObserver() {
        // given
        NodeResponse node = createNodeWithAnchorAndObservers();

        WriteRequest request = createLinkPdfFileRequest(node);

        // when
        WriteResponse response = user.write(request);

        // then
        assertThatObserverWasNotNotified(user, response);
    }

    @Test
    public void shouldSendFileOnlyToConfiguredObserver() {
        // given
        NodeResponse node = createNodeWithAnchorAndObservers(user);

        WriteRequest request = createLinkPdfFileRequest(node);

        // when
        WriteResponse response = user.write(request);

        // then
        assertThatObserverWasNotified(user, response);
        assertThatObserverWasNotNotified(getSecondUser(), response);
    }

    @Test
    public void shouldSendFileOnlyToConfiguredObserverForCamouflagedNode() {
        // given
        NodeResponse node = createCamouflagedNodeWithAnchorAndObservers(user);

        WriteRequest request = createLinkPdfFileRequest(node);

        // when
        WriteResponse response = user.write(request);

        // then
        assertThatObserverWasNotified(user, response, camouflage);
        assertThatObserverWasNotNotified(getSecondUser(), response, camouflage);
    }

    @Test
    public void shouldSendFileToMultipleObservers() {
        // given
        User user2 = getSecondUser();
        User user3 = getThirdUser();

        NodeResponse node = createNodeWithAnchorAndObservers(user, user2, user3);

        WriteRequest request = createLinkPdfFileRequest(node);

        // when
        WriteResponse response = user.write(request);

        // then
        assertThatObserverWasNotified(user, response);
        assertThatObserverWasNotified(user3, response);
        assertThatObserverWasNotified(user2, response);
    }

    @Test
    public void shouldSendFileToMultipleObserversForCamouflagedNode() {
        // given
        User user2 = getSecondUser();
        User user3 = getThirdUser();

        NodeResponse node = createCamouflagedNodeWithAnchorAndObservers(user, user2, user3);

        WriteRequest request = createLinkPdfFileRequest(node);

        // when
        WriteResponse response = user.write(request);

        // then
        assertThatObserverWasNotified(user, response, camouflage);
        assertThatObserverWasNotified(user3, response, camouflage);
        assertThatObserverWasNotified(user2, response, camouflage);
    }

    @Test
    public void shouldNotSendFileToObserverWhenCredentialsAreIncorrectAndRecoveryIsNotDefined() {
        // given
        NodeResponse node = ledger.writeNode(createNodeWithAnchor()
                .linkObserver(ObserverNodeRequest.builder()
                        .pgpKey(JON_DOE_PUBLIC_KEY)
                        .s3Storage(S3Storage.builder()
                                .bucket(OBSERVER_STORAGE_S3_BUCKET)
                                .region(OBSERVER_STORAGE_S3_BUCKET_REGION)
                                .credentials("non-existing-key", "non-existing-secret-key")
                                .path(buildPath(user)))));

        WriteRequest request = createLinkPdfFileRequest(node);

        // when
        WriteResponse response = user.write(request);

        // then
        assertThatObserverWasNotNotified(user, response);
    }

    @Test
    public void shouldSendFileToRecoveryStorageWhenCredentialsAreIncorrectAndRecoveryIsDefined() {
        // given
        var recoveryPath = UUID.randomUUID().toString();

        NodeResponse node = ledger.writeNode(createNodeWithAnchor()
                .linkObserver(ObserverNodeRequest.builder()
                        .pgpKey(JON_DOE_PUBLIC_KEY)
                        .recoveryStoragePath(recoveryPath)
                        .s3Storage(S3Storage.builder()
                                .bucket(OBSERVER_STORAGE_S3_BUCKET)
                                .region(OBSERVER_STORAGE_S3_BUCKET_REGION)
                                .credentials("non-existing-key", "non-existing-secret-key")
                                .path(buildPath(user)))));

        WriteRequest request = createLinkPdfFileRequest(node);

        // when
        WriteResponse response = user.write(request);

        // then
        assertThatObserverWasNotNotified(user, response);
        assertThatDeadLetterObserverWasNotified(recoveryPath, response);
    }

    @Test
    public void shouldSendFileToStorageOrRecovery() {
        // given
        var path1 = UUID.randomUUID().toString();
        var path2 = UUID.randomUUID().toString();
        var recoveryPath1 = UUID.randomUUID().toString();
        var recoveryPath2 = UUID.randomUUID().toString();

        NodeResponse node = ledger.writeNode(createNodeWithAnchor()
                .linkObserver(ObserverNodeRequest.builder()
                        .pgpKey(JON_DOE_PUBLIC_KEY)
                        .recoveryStoragePath(recoveryPath1)
                        .s3Storage(S3Storage.builder()
                                .bucket(OBSERVER_STORAGE_S3_BUCKET)
                                .region(OBSERVER_STORAGE_S3_BUCKET_REGION)
                                .path(path1)))
                .linkObserver(ObserverNodeRequest.builder()
                        .pgpKey(JACK_DANIELS_PUBLIC_KEY)
                        .recoveryStoragePath(recoveryPath2)
                        .s3Storage(S3Storage.builder()
                                .bucket(OBSERVER_STORAGE_S3_BUCKET)
                                .region(OBSERVER_STORAGE_S3_BUCKET_REGION)
                                .credentials("non-existing-key", "non-existing-secret-key")
                                .path(path2))));

        WriteRequest request = createLinkPdfFileRequest(node);

        // when
        WriteResponse response = user.write(request);

        // then
        assertThatObserverWasNotified(path1, response, JON_DOE_PRIVATE_KEY);
        assertThatObserverWasNotNotified(recoveryPath1, response, JON_DOE_PRIVATE_KEY);

        assertThatObserverWasNotNotified(path2, response, JACK_DANIELS_PRIVATE_KEY);
        assertThatDeadLetterObserverWasNotified(recoveryPath2, response, JACK_DANIELS_PRIVATE_KEY);
    }

    @Test
    public void fileEncryptedAndDecryptedShouldBeTheSameAsOriginal() throws IOException {
        // given
        PgpEncrypter pgpEncrypter = new PgpEncrypter();
        PgpDecrypter pgpDecrypter = new PgpDecrypter();

        // when
        try (
                InputStream file = new FileInputStream(requireNonNull(getClass().getClassLoader().getResource("pgp/example.zip")).getFile());
                ByteArrayInputStream input = new ByteArrayInputStream(IOUtils.toByteArray(file))
        ) {
            byte[] inputBytes = IOUtils.toByteArray(input);
            byte[] encryptedData = pgpEncrypter.encrypt(inputBytes, JON_DOE_PUBLIC_KEY);
            byte[] decryptedData = pgpDecrypter.decrypt(encryptedData, PgpTestKeys.JON_DOE_PRIVATE_KEY, PgpTestKeys.PASSPHRASE);

            // then
            assertThat(inputBytes).isNotEqualTo(encryptedData);
            assertThat(decryptedData).isEqualTo(inputBytes);
        }
    }

    @Test
    public void shouldFailWhenLinkingNodeCreatedInPreviousRequest() {
        // given
        NodeResponse node = createNodeWithAnchorAndObservers(user);

        NodeResponse document = user.writeNode(NodeRequest.builder()
                .type(getDocumentType())
                .file(createPdf(getDocumentText(), getDocumentData())));

        WriteRequest request = WriteRequest.builder()
                .addLinkRequest(document, node, "#documents")
                .build();

        // when
        ErrorResponse error = expectError(() -> user.write(request));

        // then
        assertThat(error).hasErrorCodeAndMessage(ErrorCode.LINKING_TO_NODE_WITH_OBSERVERS_USING_NODE_NOT_PROVIDED_IN_THIS_REQUEST);
    }

    @Test
    public void shouldUseProvidedExistingNodeApiRequestWhenNotifyingObservers() {
        // given
        NodeResponse baseNode = user.writeNode(NodeRequest.builder()
                .type(TypeNodeRequest.builder()
                        .anchor("#test")
                        .permissions(allowAllForEveryone()))
                .linkObserver(ObserverNodeRequest.builder()
                        .pgpKey(JON_DOE_PUBLIC_KEY)
                        .s3Storage(S3Storage.builder()
                                .bucket(OBSERVER_STORAGE_S3_BUCKET)
                                .region(OBSERVER_STORAGE_S3_BUCKET_REGION)
                                .path(buildPath(user)))));

        NodeRequest testRequest = NodeRequest.builder()
                .stateSource(StateStorage.INSTANCE.generateUniqueStateSource())
                .type(TypeNodeRequest.builder()
                        .stateProperties("value"))
                .data("value", "example_value")
                .build();

        NodeResponse testNode = user.writeNode(testRequest);

        WriteRequest request = WriteRequest.builder()
                .addLinkRequest(testNode, baseNode, "#test")
                .addExistingNodeRequest(testNode.getId(), testRequest)
                .build();

        // when
        WriteResponse response = user.write(request);

        // then
        assertThatObserverWasNotifiedWithApiRequest(user, response.getLinks().get(0), testNode, testRequest);
    }

    @Test
    public void shouldUseProvidedExistingNodeFileUploadWhenNotifyingObservers() {
        // given
        NodeResponse baseNode = createNodeWithAnchorAndObservers(user);

        NodeRequest testRequest = NodeRequest.builder()
                .stateSource(StateStorage.INSTANCE.generateUniqueStateSource())
                .type(getDocumentType())
                .file(createPdf(getDocumentText(), getDocumentData()))
                .build();

        NodeResponse testNode = user.writeNode(testRequest);

        WriteRequest request = WriteRequest.builder()
                .addLinkRequest(testNode, baseNode, "#documents")
                .addExistingNodeRequest(testNode.getId(), testRequest)
                .build();

        // when
        WriteResponse response = user.write(request);

        // then
        assertThatObserverWasNotified(user, response.getLinks().get(0), testNode);
    }

    @Test
    public void shouldReturnErrorWhenProvidedExistingNodeApiRequestDoesNotMatchObjectHash() {
        // given
        NodeResponse baseNode = user.writeNode(NodeRequest.builder()
                .type(TypeNodeRequest.builder()
                        .anchor("#test")
                        .permissions(allowAllForEveryone()))
                .linkObserver(ObserverNodeRequest.builder()
                        .pgpKey(JON_DOE_PUBLIC_KEY)
                        .s3Storage(S3Storage.builder()
                                .bucket(OBSERVER_STORAGE_S3_BUCKET)
                                .region(OBSERVER_STORAGE_S3_BUCKET_REGION)
                                .path(buildPath(user)))));

        NodeRequest testRequest = NodeRequest.builder()
                .stateSource(StateStorage.INSTANCE.generateUniqueStateSource())
                .type(TypeNodeRequest.builder()
                        .stateProperties("value"))
                .data("value", "example_value")
                .build();

        NodeResponse testNode = user.writeNode(testRequest);

        NodeRequest modifiedTestRequest = NodeRequest.builder()
                .stateSource(testRequest.getStateSource())
                .type(TypeNodeRequest.builder()
                        .stateProperties("value"))
                .data("value", "modified_value")
                .build();

        WriteRequest request = WriteRequest.builder()
                .addLinkRequest(testNode, baseNode, "#test")
                .addExistingNodeRequest(testNode.getId(), modifiedTestRequest)
                .build();

        // when
        ErrorResponse error = expectError(() -> user.write(request));

        // then
        assertThat(error).hasErrorCodeAndMessage(ErrorCode.OBJECT_HASH_MISMATCH, testNode.getId());
    }

    @Test
    public void shouldReturnErrorWhenProvidedExistingNodeFileUploadDoesNotMatchObjectHash() {
        // given
        NodeResponse baseNode = createNodeWithAnchorAndObservers(user);

        NodeRequest testRequest = NodeRequest.builder()
                .stateSource(StateStorage.INSTANCE.generateUniqueStateSource())
                .type(getDocumentType())
                .file(createPdf(getDocumentText(), getDocumentData()))
                .build();

        NodeResponse testNode = user.writeNode(testRequest);

        NodeRequest modifiedTestRequest = NodeRequest.builder()
                .stateSource(testRequest.getStateSource())
                .type(getDocumentType())
                .file(createPdf("modified document text", getDocumentData()))
                .build();

        WriteRequest request = WriteRequest.builder()
                .addLinkRequest(testNode, baseNode, "#documents")
                .addExistingNodeRequest(testNode.getId(), modifiedTestRequest)
                .build();

        // when
        ErrorResponse error = expectError(() -> user.write(request));

        // then
        assertThat(error).hasErrorCodeAndMessage(ErrorCode.FILE_HASH_MISMATCH, testNode.getId());
    }

    @Test
    public void shouldValidateObserverData() {
        // when
        var error = expectError(() -> ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .linkObserver(ObserverNodeRequest.builder()
                        .pgpKey(""))));

        // then
        assertThat(error).hasErrorCode(ErrorCode.VALIDATION_ERROR)
                .hasFieldError("requests[0].#observers[0].pgp_key", "not_empty")
                .hasFieldError("requests[0].#observers[0].aws_s3", "not_null");

        // when
        error = expectError(() -> ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .linkObserver(ObserverNodeRequest.builder()
                        .pgpKey("example-pgp-key")
                        .recoveryStoragePath("")
                        .s3Storage(S3Storage.builder()
                                .region("")
                                .bucket("")
                                .path("")
                                .credentials("", "")))));

        // then
        assertThat(error).hasErrorCode(ErrorCode.VALIDATION_ERROR)
                .hasFieldError("requests[0].#observers[0].recovery_storage.path", "not_empty")
                .hasFieldError("requests[0].#observers[0].aws_s3.region", "not_empty")
                .hasFieldError("requests[0].#observers[0].aws_s3.bucket", "not_empty")
                .hasFieldError("requests[0].#observers[0].aws_s3.path", "not_empty")
                .hasFieldError("requests[0].#observers[0].aws_s3.credentials.access_key_id", "not_empty")
                .hasFieldError("requests[0].#observers[0].aws_s3.credentials.secret_access_key", "not_empty");
    }

    private static TypeNodeRequest getDocumentType() {
        return TypeNodeRequest.builder()
                .linkTemplate(TemplateNodeRequest.builder()
                        .text(getTemplateText()))
                .build();
    }

    public static WriteRequest createLinkPdfFileRequest(NodeResponse node) {
        return WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(getDocumentType())
                        .file(createPdf(getDocumentText(), getDocumentData())))
                .addLinkRequest(0, node, "#documents")
                .build();
    }

    private Builder<?> createNodeWithAnchor() {
        return NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#documents", getDocumentType())
                        .permission(allowAllForEveryone()));
    }

    private NodeResponse createNodeWithAnchorAndObservers(User... users) {
        NodeRequest.Builder<?> requestBuilder = createNodeWithAnchor();
        for (User user : users) {
            requestBuilder.linkObserver(ObserverNodeRequest.builder()
                    .pgpKey(JON_DOE_PUBLIC_KEY)
                    .s3Storage(S3Storage.builder()
                            .bucket(OBSERVER_STORAGE_S3_BUCKET)
                            .region(OBSERVER_STORAGE_S3_BUCKET_REGION)
                            .path(buildPath(user))));
        }

        return user.writeNode(requestBuilder);
    }

    private NodeResponse createCamouflagedNodeWithAnchorAndObservers(User... users) {
        NodeRequest.Builder<?> requestBuilder = createNodeWithAnchor();
        for (User user : users) {
            requestBuilder.linkObserver(ObserverNodeRequest.builder()
                    .pgpKey(JON_DOE_PUBLIC_KEY)
                    .s3Storage(S3Storage.builder()
                            .bucket(OBSERVER_STORAGE_S3_BUCKET)
                            .region(OBSERVER_STORAGE_S3_BUCKET_REGION)
                            .path(buildPath(user))))
                    .camouflage(camouflage);
        }

        return user.writeNode(requestBuilder);
    }

    public static void assertThatObserverWasNotified(User user, WriteResponse response) {
        assertThatObserverWasNotified(buildPath(user), response, JON_DOE_PRIVATE_KEY, null);
    }

    public static void assertThatObserverWasNotified(User user, LinkResponse linkResponse, NodeResponse nodeResponse) {
        assertThatObserverWasNotified(buildPath(user), linkResponse, nodeResponse, JON_DOE_PRIVATE_KEY, null);
    }

    private static void assertThatObserverWasNotified(User user, WriteResponse response, Camouflage camouflage) {
        assertThatObserverWasNotified(buildPath(user), response, JON_DOE_PRIVATE_KEY, camouflage);
    }

    private static void assertThatObserverWasNotified(String path, WriteResponse response) {
        assertThatObserverWasNotified(path, response, JON_DOE_PRIVATE_KEY, null);
    }

    private static void assertThatObserverWasNotified(String path, WriteResponse response, String privateKey) {
        assertThatObserverWasNotified(path, response, privateKey, null);
    }

    private static void assertThatObserverWasNotified(String path, WriteResponse response, String privateKey, Camouflage camouflage) {
        var linkResponse = response.getLinks().get(0);
        var nodeResponse = response.getNodes().get(0);

        assertThatObserverWasNotified(path, linkResponse, nodeResponse, privateKey, camouflage);
    }

    private static void assertThatObserverWasNotified(String path, LinkResponse linkResponse, NodeResponse nodeResponse, String privateKey, Camouflage camouflage) {
        var observerFile = camouflage != null
                ? new ObserverFile(OBSERVER_STORAGE_S3_BUCKET, path, linkResponse, camouflage)
                : new ObserverFile(OBSERVER_STORAGE_S3_BUCKET, path, linkResponse);

        var uploadedZipOpt = observerFile.get(privateKey, PgpTestKeys.PASSPHRASE);

        assertThat(uploadedZipOpt).isPresent();
        var uploaded = uploadedZipOpt.get();

        assertUploadedData(uploaded, getDocumentText(), getDocumentData(), nodeResponse);
    }

    private static void assertThatDeadLetterObserverWasNotified(String path, WriteResponse response) {
        assertThatDeadLetterObserverWasNotified(path, response, JON_DOE_PRIVATE_KEY);
    }

    private static void assertThatDeadLetterObserverWasNotified(String path, WriteResponse response, String privateKey) {
        var linkResponse = response.getLinks().get(0);
        var nodeResponse = response.getNodes().get(0);

        assertThatDeadLetterObserverWasNotified(path, linkResponse, nodeResponse, privateKey, null);
    }

    private static void assertThatDeadLetterObserverWasNotified(String path, LinkResponse linkResponse, NodeResponse nodeResponse, String privateKey, Camouflage camouflage) {
        var observerFile = camouflage != null
                ? new ObserverFile(OBSERVER_DEAD_LETTER_BUCKET, path, linkResponse, camouflage)
                : new ObserverFile(OBSERVER_DEAD_LETTER_BUCKET, path, linkResponse);

        var uploadedZipOpt = observerFile.get(privateKey, PgpTestKeys.PASSPHRASE);

        assertThat(uploadedZipOpt).isPresent();
        var uploaded = uploadedZipOpt.get();

        assertUploadedData(uploaded, getDocumentText(), getDocumentData(), nodeResponse);
    }

    private static void assertThatObserverWasNotifiedWithApiRequest(User user, LinkResponse linkResponse, NodeResponse nodeResponse, NodeRequest nodeRequest) {
        var observerFile = new ObserverFile(OBSERVER_STORAGE_S3_BUCKET, buildPath(user), linkResponse);
        var uploadedZipOpt = observerFile.get(JON_DOE_PRIVATE_KEY, PgpTestKeys.PASSPHRASE);

        assertThat(uploadedZipOpt).isPresent();
        var uploaded = uploadedZipOpt.get();

        assertUploadedData(uploaded, null, UncheckedObjectMapper.MAPPER.convertValue(nodeRequest, Map.class), nodeResponse);
    }

    private static void assertUploadedData(DataUploadedToS3 uploadedData, String expectedPdfText, Map<?, ?> expectedObjectJson, NodeResponse expectedNodeResponse) {
        assertThat(uploadedData.getPdfText()).isEqualToNormalizingWhitespace(expectedPdfText);
        assertThat(uploadedData.getObjectJson()).isEqualTo(expectedObjectJson);
        assertThat(uploadedData.getNodeJson()).satisfies(nodeJson -> {
            assertThat(nodeJson.getId()).isEqualTo(expectedNodeResponse.getId());
            assertThat(nodeJson.getType()).isEqualTo(expectedNodeResponse.getType());
            assertThat(nodeJson.getCreated()).isEqualTo(expectedNodeResponse.getCreated());
            assertThat(nodeJson.getFileSha3()).isEqualTo(expectedNodeResponse.getFileSha3());
            assertThat(nodeJson.getObjectSha3()).isEqualTo(expectedNodeResponse.getObjectSha3());
            assertThat(nodeJson.getStateSha3()).isEqualTo(expectedNodeResponse.getStateSha3());
            assertThat(nodeJson.getCamouflageSha3()).isEqualTo(expectedNodeResponse.getCamouflageSha3());
            assertThat(nodeJson.getState()).containsExactlyEntriesOf(expectedNodeResponse.getState());
        });
    }

    private static void assertThatObserverWasNotNotified(User user, WriteResponse response) {
        assertThatObserverWasNotNotified(buildPath(user), response, JON_DOE_PRIVATE_KEY, null);
    }

    private static void assertThatObserverWasNotNotified(User user, WriteResponse response, Camouflage camouflage) {
        assertThatObserverWasNotNotified(buildPath(user), response, JON_DOE_PRIVATE_KEY, camouflage);
    }

    private static void assertThatObserverWasNotNotified(String path, WriteResponse response, String privateKey) {
        assertThatObserverWasNotNotified(path, response, privateKey, null);
    }

    private static void assertThatObserverWasNotNotified(String path, WriteResponse response, String privateKey, Camouflage camouflage) {
        var linkResponse = response.getLinks().get(0);

        var observerFile = camouflage != null
                ? new ObserverFile(OBSERVER_STORAGE_S3_BUCKET, path, linkResponse, camouflage)
                : new ObserverFile(OBSERVER_STORAGE_S3_BUCKET, path, linkResponse);

        var uploadedZipOpt = observerFile.get(privateKey, PgpTestKeys.PASSPHRASE);

        assertThat(uploadedZipOpt).isNotPresent();
    }

    private static String buildPath(User user) {
        return user.getUsername();
    }
}
