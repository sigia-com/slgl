package io.slgl.api.it;

import io.slgl.api.it.data.PgpTestKeys;
import io.slgl.api.it.properties.Props;
import io.slgl.api.utils.ErrorCode;
import io.slgl.client.error.ErrorResponse;
import io.slgl.client.node.*;
import io.slgl.client.storage.S3Storage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.it.data.PermissionMother.allowAllForEveryone;
import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;

@Slf4j
public class ObserversStorageValidationIT extends AbstractApiTest {

    private final String observerS3Bucket = Props.getSlglProperties().getObserverStorageS3Bucket();
    
    @Test
    public void shouldSuccessfullySaveWithInlineObservers() {
        // given
        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .linkObserver(ObserverNodeRequest.builder()
                        .pgpKey(PgpTestKeys.JON_DOE_PUBLIC_KEY)
                        .recoveryStoragePath("observer-recovery-path1")
                        .s3Storage(S3Storage.builder()
                                .bucket(observerS3Bucket)
                                .path("path1")
                                .region("eu-west-1")
                        ))
                .linkObserver(ObserverNodeRequest.builder()
                        .pgpKey(PgpTestKeys.JON_DOE_PUBLIC_KEY)
                        .recoveryStoragePath("observer-recovery-path2")
                        .s3Storage(S3Storage.builder()
                                .bucket(observerS3Bucket)
                                .path("path2")
                                .region("eu-west-1")
                        ))
                .build();

        // when
        NodeResponse response = ledger.writeNode(request);

        // then
        assertThat(response.getId()).isEqualTo(request.getId());
    }

    @Test
    public void shouldSuccessfullySaveWithTheSamePathDifferentBucketInlineObservers() {
        // given
        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .linkObserver(ObserverNodeRequest.builder()
                        .pgpKey(PgpTestKeys.JON_DOE_PUBLIC_KEY)
                        .recoveryStoragePath("observer-recovery-path1")
                        .s3Storage(S3Storage.builder()
                                .bucket("example-bucket-1")
                                .path("path")
                                .region("eu-west-1")
                        ))
                .linkObserver(ObserverNodeRequest.builder()
                        .pgpKey(PgpTestKeys.JON_DOE_PUBLIC_KEY)
                        .recoveryStoragePath("observer-recovery-path2")
                        .s3Storage(S3Storage.builder()
                                .bucket("example-bucket-2")
                                .path("path")
                                .region("eu-west-1")
                        ))
                .build();

        // when
        NodeResponse response = ledger.writeNode(request);

        // then
        assertThat(response.getId()).isEqualTo(request.getId());
    }

    @Test
    public void shouldSuccessfullyLinkToNodeWithInlineObservers() {
        // given
        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#entry")
                        .permission(allowAllForEveryone()))
                .linkObserver(ObserverNodeRequest.builder()
                        .pgpKey(PgpTestKeys.JON_DOE_PUBLIC_KEY)
                        .recoveryStoragePath("observer-recovery-path1")
                        .s3Storage(S3Storage.builder()
                                .bucket(observerS3Bucket)
                                .path("path1")
                                .region("eu-west-1")
                        ))
                .linkObserver(ObserverNodeRequest.builder()
                        .pgpKey(PgpTestKeys.JON_DOE_PUBLIC_KEY)
                        .recoveryStoragePath("observer-recovery-path2")
                        .s3Storage(S3Storage.builder()
                                .bucket(observerS3Bucket)
                                .path("path2")
                                .region("eu-west-1")
                        )));

        // when
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#entry"));

        // then
        assertThat(response).isNotNull();
    }

    @Test
    public void shouldSuccessfullySaveWithStandaloneObservers() {
        // given
        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .permission(allowAllForEveryone())));

        WriteRequest observer1 = WriteRequest.builder()
                .addRequest(ObserverNodeRequest.builder()
                        .id(generateUniqueId())
                        .pgpKey(PgpTestKeys.JON_DOE_PUBLIC_KEY)
                        .recoveryStoragePath("observer-recovery-path1")
                        .s3Storage(S3Storage.builder()
                                .bucket(observerS3Bucket)
                                .path("path1")
                                .region("eu-west-1")))

                .addLinkRequest(0, node, "#observers")
                .build();

        WriteRequest observer2 = WriteRequest.builder()
                .addRequest(ObserverNodeRequest.builder()
                        .id(generateUniqueId())
                        .pgpKey(PgpTestKeys.JON_DOE_PUBLIC_KEY)
                        .recoveryStoragePath("observer-recovery-path2")
                        .s3Storage(S3Storage.builder()
                                .bucket(observerS3Bucket)
                                .path("path2")
                                .region("eu-west-1")
                        ))
                .addLinkRequest(0, node, "#observers")
                .build();

        // when
        ledger.write(observer1);
        ledger.write(observer2);
    }

    @Test
    public void shouldSuccessfullyLinkToNodeWithStandaloneObservers() {
        // given
        NodeResponse type = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#entry")
                .permission(allowAllForEveryone()));

        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(type));

        ledger.write(WriteRequest.builder()
                .addRequest(ObserverNodeRequest.builder()
                        .id(generateUniqueId())
                        .pgpKey(PgpTestKeys.JON_DOE_PUBLIC_KEY)
                        .recoveryStoragePath("observer-recovery-path1")
                        .s3Storage(S3Storage.builder()
                                .bucket(observerS3Bucket)
                                .path("path1")
                                .region("eu-west-1")
                        ))
                .addLinkRequest(0, node, "#observers"));

        ledger.write(WriteRequest.builder()
                .addRequest(ObserverNodeRequest.builder()
                        .id(generateUniqueId())
                        .pgpKey(PgpTestKeys.JON_DOE_PUBLIC_KEY)
                        .recoveryStoragePath("observer-recovery-path2")
                        .s3Storage(S3Storage.builder()
                                .bucket(observerS3Bucket)
                                .path("path2")
                                .region("eu-west-1")
                        ))
                .addLinkRequest(0, node, "#observers"));

        // when
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#entry"));

        // then
        assertThat(response).isNotNull();
    }

    @Test
    public void shouldFailWithRecoveryPathInlineObservers() {
        // given
        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .linkObserver(ObserverNodeRequest.builder()
                        .pgpKey(PgpTestKeys.JON_DOE_PUBLIC_KEY)
                        .recoveryStoragePath("observer-recovery-path")
                        .s3Storage(S3Storage.builder()
                                .bucket(observerS3Bucket)
                                .path("path1")
                                .region("eu-west-1")
                        ))
                .linkObserver(ObserverNodeRequest.builder()
                        .pgpKey(PgpTestKeys.JON_DOE_PUBLIC_KEY)
                        .recoveryStoragePath("observer-recovery-path")
                        .s3Storage(S3Storage.builder()
                                .bucket(observerS3Bucket)
                                .path("path2")
                                .region("eu-west-1")
                        ))
                .build();

        // when
        ErrorResponse errorResponse = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(errorResponse).hasErrorCodeAndMessage(ErrorCode.OBSERVER_UNIQUE_RECOVERY_STORAGE_PATHS, "observer-recovery-path");
    }

    @Test
    public void shouldFailWithStoragePathInlineObservers() {
        // given
        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .linkObserver(ObserverNodeRequest.builder()
                        .pgpKey(PgpTestKeys.JON_DOE_PUBLIC_KEY)
                        .recoveryStoragePath("observer-recovery-path1")
                        .s3Storage(S3Storage.builder()
                                .bucket(observerS3Bucket)
                                .path("path")
                                .region("eu-west-1")
                        ))
                .linkObserver(ObserverNodeRequest.builder()
                        .pgpKey(PgpTestKeys.JON_DOE_PUBLIC_KEY)
                        .s3Storage(S3Storage.builder()
                                .bucket(observerS3Bucket)
                                .path("path")
                                .region("eu-west-1")
                        ))
                .build();

        // when
        ErrorResponse errorResponse = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(errorResponse).hasErrorCodeAndMessage(ErrorCode.OBSERVER_UNIQUE_STORAGE_LOCATIONS, "aws_s3/" + observerS3Bucket + "/path");
    }

    @Test
    public void shouldFailWithRecoveryPathStandaloneObservers() {
        // given
        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .permission(allowAllForEveryone())));

        ledger.write(WriteRequest.builder()
                .addRequest(ObserverNodeRequest.builder()
                        .id(generateUniqueId())
                        .pgpKey(PgpTestKeys.JON_DOE_PUBLIC_KEY)
                        .recoveryStoragePath("observer-recovery-path")
                        .s3Storage(S3Storage.builder()
                                .bucket(observerS3Bucket)
                                .path("path1")
                                .region("eu-west-1")))
                .addLinkRequest(0, node, "#observers"));

        // when
        ErrorResponse errorResponse = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(ObserverNodeRequest.builder()
                        .id(generateUniqueId())
                        .pgpKey(PgpTestKeys.JON_DOE_PUBLIC_KEY)
                        .recoveryStoragePath("observer-recovery-path")
                        .s3Storage(S3Storage.builder()
                                .bucket(observerS3Bucket)
                                .path("path2")
                                .region("eu-west-1")))
                .addLinkRequest(0, node, "#observers")));

        // then
        assertThat(errorResponse).hasErrorCodeAndMessage(ErrorCode.OBSERVER_UNIQUE_RECOVERY_STORAGE_PATHS, "observer-recovery-path");
    }

    @Test
    public void shouldFailWithStoragePathStandaloneObservers() {
        // given
        NodeResponse entry = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .permission(allowAllForEveryone())));

        ledger.write(WriteRequest.builder()
                .addRequest(ObserverNodeRequest.builder()
                        .id(generateUniqueId())
                        .pgpKey(PgpTestKeys.JON_DOE_PUBLIC_KEY)
                        .recoveryStoragePath("observer-recovery-path1")
                        .s3Storage(S3Storage.builder()
                                .bucket(observerS3Bucket)
                                .path("path")
                                .region("eu-west-1")
                        ))
                .addLinkRequest(0, entry, "#observers"));

        // when
        ErrorResponse errorResponse = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(ObserverNodeRequest.builder()
                        .id(generateUniqueId())
                        .pgpKey(PgpTestKeys.JON_DOE_PUBLIC_KEY)
                        .recoveryStoragePath("observer-recovery-path2")
                        .s3Storage(S3Storage.builder()
                                .bucket(observerS3Bucket)
                                .path("path")
                                .region("eu-west-1")
                        ))
                .addLinkRequest(0, entry, "#observers")));

        // then
        assertThat(errorResponse).hasErrorCodeAndMessage(ErrorCode.OBSERVER_UNIQUE_STORAGE_LOCATIONS, "aws_s3/" + observerS3Bucket + "/path");
    }
}

