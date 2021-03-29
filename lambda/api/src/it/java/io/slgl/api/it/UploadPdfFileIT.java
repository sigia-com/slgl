package io.slgl.api.it;

import com.google.common.collect.ImmutableMap;
import io.slgl.api.it.data.DocumentMother;
import io.slgl.api.it.data.PdfMother;
import io.slgl.api.utils.ErrorCode;
import io.slgl.client.Types;
import io.slgl.client.camouflage.Camouflage;
import io.slgl.client.node.*;
import io.slgl.client.node.permission.Permission;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.it.data.DocumentMother.createPdfDocument;
import static io.slgl.api.it.data.DocumentMother.getDocumentText;
import static io.slgl.api.it.data.PermissionMother.allowAllForEveryone;
import static io.slgl.api.it.data.PermissionMother.allowReadStateForEveryone;
import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;
import static io.slgl.api.it.utils.HashUtils.sha3_512;
import static io.slgl.client.node.NodeTypeId.simple;
import static io.slgl.client.node.permission.Allow.allowLink;
import static io.slgl.client.node.permission.Requirement.requireThat;

public class UploadPdfFileIT extends AbstractApiTest {

    @Test
    public void shouldUploadPdfFile() {
        // given
        NodeRequest request = NodeRequest.builder()
                .file(createPdfDocument())
                .build();

        // when
        NodeResponse response = user.writeNode(request);

        // then
        assertThat(response.getId()).isNotEmpty();
        assertThat(response.getFileSha3()).isEqualTo(sha3_512(request.getFile()));
        assertThat(response.getObjectSha3()).isNull();
        assertThat(response.getStateSha3()).isNotNull();
        assertThat(response.getState()).containsKey("@file");
    }

    @Test
    public void shouldUploadPdfFileWithProvidedNodeId() {
        // given
        String nodeId = generateUniqueId();

        NodeRequest request = NodeRequest.builder()
                .id(nodeId)
                .file(createPdfDocument())
                .build();

        // when
        NodeResponse response = user.writeNode(request);

        // then
        assertThat(response.getId()).isEqualTo(nodeId);
        assertThat(response.getFileSha3()).isEqualTo(sha3_512(request.getFile()));
        assertThat(response.getObjectSha3()).isNull();
    }

    @Test
    public void shouldUploadPdfFileWithCamouflaged() {
        // given
        NodeResponse type = user.writeNode(TypeNodeRequest.builder()
                .anchor("#anchor")
                .permission(allowAllForEveryone()));

        NodeRequest request = NodeRequest.builder()
                .type(type)
                .file(createPdfDocument())
                .camouflage(Camouflage.builder()
                        .anchor("#observersC", "#observers")
                        .anchor("#auditorsC", "#auditors")
                        .anchor("#anchorC", "#anchor"))
                .build();

        // when
        NodeResponse response = user.writeNode(request);

        // then
        assertThat(response.getType()).isEqualTo(simple(Types.CAMOUFLAGE));
        assertThat(response.getCamouflageSha3()).isNotNull();
        assertThat(response.getFileSha3()).isEqualTo(sha3_512(request.getFile()));
        assertThat(response.getObjectSha3()).isNull();
    }

    @Test
    public void shouldIgnoreTypeAddedInPdfMetadata() {
        // given
        NodeResponse documentType = user.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .stateProperties("name", "value")
                .linkTemplate(TemplateNodeRequest.builder()
                        .text(DocumentMother.getTemplateText())));

        Map<String, Object> documentData = ImmutableMap.<String, Object>builder()
                .putAll(DocumentMother.getDocumentData())
                .put("@type", documentType.getId())
                .build();

        NodeRequest request = NodeRequest.builder()
                .file(PdfMother.createPdf(getDocumentText(), documentData))
                .build();

        // when
        NodeResponse response = user.writeNode(request);

        // then
        assertThat(response.getFileSha3()).isEqualTo(sha3_512(request.getFile()));
        assertThat(response.getObjectSha3()).isNull();
        assertThat(response.getState())
                .doesNotContainKeys(documentData.keySet().toArray(new String[]{}));
    }

    @Test
    public void shouldUploadPdfFileWithoutJsonObjectInMetadata() throws IOException {
        // given
        byte[] document = PdfMother.createPdf("Example agreement.");
        assertThat(PDDocument.load(document).getDocumentInformation().getMetadataKeys()).doesNotContain("json_metadata");

        var request = NodeRequest.builder()
                .file(document)
                .build();

        // when
        NodeResponse response = ledger.writeNode(request);

        // then
        assertThat(response.getFileSha3()).isEqualTo(sha3_512(request.getFile()));
        assertThat(response.getObjectSha3()).isNull();
    }

    @Test
    public void shouldFailWhenUploadingInvalidPdfFile() {
        // given
        var documentType = user.writeNode(TypeNodeRequest.builder()
                        .linkTemplate(TemplateNodeRequest.builder()
                                .text("Some template")));

        var request = NodeRequest.builder()
                .type(documentType)
                .file("not-a-pdf-bytes".getBytes())
                .build();

        // when
        var error = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(error).hasErrorCode(ErrorCode.LINKED_DOCUMENT_MUST_BE_PDF);
    }

    @Test
    public void shouldFailWhenUploadingPdfFileWithInvalidJsonObject() {
        // given
        var documentType = user.writeNode(TypeNodeRequest.builder()
                .linkTemplate(TemplateNodeRequest.builder()
                        .text("Example agreement.")));

        byte[] document = PdfMother.createPdf("Example agreement.", "invalid-json-object");

        var request = NodeRequest.builder()
                .type(documentType)
                .file(document)
                .build();

        // when
        var error = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(error).hasErrorCode(ErrorCode.UNABLE_TO_PARSE_REQUEST);
    }

    @Test
    void shouldProperlyIncludeSignatureDetailsInPermissionContext() {
        OffsetDateTime beforeSign = OffsetDateTime.now().minus(2, ChronoUnit.SECONDS);
        byte[] pdf = PdfMother.createSignedPdf("text");
        OffsetDateTime afterSign = OffsetDateTime.now().plus(2, ChronoUnit.SECONDS);

        NodeResponse testNode = user.writeNode(NodeRequest.builder()
                .type(TypeNodeRequest.builder()
                        .anchor("#anchor")
                        .permission(allowReadStateForEveryone())
                        .permission(Permission.builder()
                                .allow(allowLink("#anchor"))
                                .requireAll(
                                        requireThat("$source_node.$file.document_signatures").atLeastOne().as("signature").meetsAllRequirements(
                                                requireThat("signature.covers_whole_document").isEqualTo().value(true),
                                                requireThat("signature.digest_algorithm").isEqualTo().value("SHA256"),
                                                requireThat("signature.encryption_algorithm").isEqualTo().value("RSA"),
                                                requireThat("signature.filter").isEqualTo().value("Adobe.PPKLite"),
                                                requireThat("signature.is_qualified").isEqualTo().value(false),
                                                requireThat("signature.sign_date").isAfter().value(beforeSign),
                                                requireThat("signature.sign_date").isBefore().value(afterSign),
                                                requireThat("signature.signature_algorithm").isEqualTo().value("RSA_SHA256"),
                                                requireThat("signature.signature_qualification").isEqualTo().value("N/A"),
                                                requireThat("signature.sub_filter").isEqualTo().value("ETSI.CAdES.detached"),
                                                requireThat("signature.validation_failed").isEqualTo().value(false),
                                                requireThat("signature.validation_passed").isEqualTo().value(false),

                                                requireThat("signature.certificate.issuer.C").isEqualTo().value("CH"),
                                                requireThat("signature.certificate.issuer.CN").isEqualTo().value("SLGL Integration Test"),
                                                requireThat("signature.certificate.self_issued").isEqualTo().value(true),
                                                requireThat("signature.certificate.self_signed").isEqualTo().value(true),
                                                requireThat("signature.certificate.serial_number").isEqualTo().value("631505640925473358629194458243374015467991461474"),
                                                requireThat("signature.certificate.subject.C").isEqualTo().value("CH"),
                                                requireThat("signature.certificate.subject.CN").isEqualTo().value("SLGL Integration Test"),
                                                requireThat("signature.certificate.valid_at_sign_time").isEqualTo().value(true),
                                                requireThat("signature.certificate.valid_now").isEqualTo().value(true)
                                        )
                                ))));
        user.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .file(pdf))
                .addLinkRequest(0, testNode, "#anchor")
                .build());
    }
}
