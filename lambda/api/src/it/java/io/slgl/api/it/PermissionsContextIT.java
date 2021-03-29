package io.slgl.api.it;

import io.slgl.api.it.assertj.SlglAssertions;
import io.slgl.api.it.data.PdfMother;
import io.slgl.api.utils.ErrorCode;
import io.slgl.client.error.ErrorResponse;
import io.slgl.client.node.*;
import io.slgl.client.node.permission.Permission;
import io.slgl.client.utils.PdfUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.it.data.PermissionMother.allowReadStateForEveryone;
import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;
import static io.slgl.api.it.utils.MapUtils.get;
import static io.slgl.client.node.permission.Allow.allowLink;
import static io.slgl.client.node.permission.Requirement.requireThat;

public class PermissionsContextIT extends AbstractApiTest {

    @Test
    public void shouldCheckTargetAndSourceNodeWhenLinking() {
        // given
        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .stateProperties("expected_value")
                        .anchor("#test")
                        .permission(allowReadStateForEveryone())
                        .permission(Permission.builder()
                                .allow(allowLink("#test"))
                                .requireAll(requireThat("$target_node.expected_value").isEqualTo().ref("$source_node.linked_value"))))
                .data("expected_value", 1));

        // when
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(TypeNodeRequest.builder()
                                .stateProperties("linked_value")
                                .permission(allowReadStateForEveryone()))
                        .data("linked_value", 1))
                .addLinkRequest(0, node, "#test"));

        // then
        assertThat(response).isNotNull();

        // when
        ErrorResponse error = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(TypeNodeRequest.builder()
                                .stateProperties("linked_value")
                                .permission(allowReadStateForEveryone()))
                        .data("linked_value", 0))
                .addLinkRequest(0, node, "#test")));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldCheckTargetAndSourceNodeWhenInlineLinking() {
        // given
        NodeResponse type = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .stateProperties("expected_value")
                .anchor("#test")
                .permission(allowReadStateForEveryone())
                .permission(Permission.builder()
                        .allow(allowLink("#test"))
                        .requireAll(requireThat("$target_node.expected_value").isEqualTo().ref("$source_node.linked_value"))));

        // when
        NodeResponse response = ledger.writeNode(NodeRequest.builder()
                .type(type)
                .data("expected_value", 1)
                .linkNode("#test", NodeRequest.builder()
                        .data("linked_value", 1)));

        // then
        assertThat(response).isNotNull();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(NodeRequest.builder()
                .type(type)
                .data("expected_value", 1)
                .linkNode("#test", NodeRequest.builder()
                        .data("linked_value", 0))));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldCheckNodesLinkedToAnchor() {
        // given
        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#anchor")
                        .anchor("#test")
                        .permission(Permission.builder()
                                .allow(allowLink("#anchor"))
                                .alwaysAllowed())
                        .permission(Permission.builder()
                                .allow(allowLink("#test"))
                                .requireAll(requireThat("$target_node.#anchor.$length").isGreaterThan().value(0)))));

        // when
        ErrorResponse error = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#test")));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        // given
        ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#anchor"));

        // when
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#test"));

        // then
        assertThat(response).isNotNull();
    }

    @Test
    public void shouldCheckNodesInlineLinkedToAnchor() {
        // given
        NodeResponse type = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#anchor")
                .anchor("#test")
                .permission(Permission.builder()
                        .allow(allowLink("#anchor"))
                        .alwaysAllowed())
                .permission(Permission.builder()
                        .allow(allowLink("#test"))
                        .requireAll(requireThat("$target_node.#anchor.$length").isGreaterThan().value(0))));

        NodeResponse nodeWithoutInlineLink = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(type));

        // when
        ErrorResponse error = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, nodeWithoutInlineLink, "#test")));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        // given
        NodeResponse nodeWithInlineLink = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(type)
                .linkNode("#anchor", NodeRequest.builder()));

        // when
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, nodeWithInlineLink, "#test"));

        // then
        assertThat(response).isNotNull();
    }

    @Test
    public void shouldCheckVisualContentHashOfPdfDocument() throws IOException {
        // given
        byte[] pdf = PdfMother.createPdf("Example document.");
        byte[] otherPdf = PdfMother.createPdf("Other document.");

        String pdfContentHash = PdfUtils.getVisualContentHashHex(pdf);

        NodeResponse documentType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .permission(allowReadStateForEveryone()));

        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#anchor")
                        .permission(Permission.builder()
                                .allow(allowLink("#anchor"))
                                .requireAll(
                                        requireThat("$source_node.$file.content_sha3").isEqualTo().value(pdfContentHash)
                                ))));

        // when
        NodeResponse fileNode = ledger.writeNode(NodeRequest.builder()
                .type(documentType)
                .file(pdf));
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addLinkRequest(fileNode, node, "#anchor"));

        // then
        assertThat(fileNode.getState()).containsKey("@file");
        assertThat(response).isNotNull();

        // when
        response = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(documentType)
                        .file(pdf))
                .addLinkRequest(0, node, "#anchor"));

        // then
        assertThat(response).isNotNull();

        // when
        NodeResponse otherFileNode = ledger.writeNode(NodeRequest.builder()
                .type(documentType)
                .file(otherPdf));

        ErrorResponse error = expectError(() -> ledger.write(WriteRequest.builder()
                .addLinkRequest(otherFileNode, node, "#anchor")));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        // when
        error = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(documentType)
                        .file(otherPdf))
                .addLinkRequest(0, node, "#anchor")));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldCheckCertificatesFromSignedPdfDocument() {
        // given
        byte[] signedPdf = PdfMother.createSignedPdf("Example document.");
        byte[] otherPdf = PdfMother.createPdf("Other document.");

        NodeResponse documentType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .permission(allowReadStateForEveryone()));

        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#anchor")
                        .permission(Permission.builder()
                                .allow(allowLink("#anchor"))
                                .requireAll(
                                        requireThat("$source_node.$file.document_signatures").atLeastOne().meetsAllRequirements(
                                                requireThat("$current.certificate.subject.CN").isEqualTo().value("SLGL Integration Test")
                                        )
                                ))));

        // when
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(documentType)
                        .file(signedPdf))
                .addLinkRequest(0, node, "#anchor"));

        // then
        assertThat(response).isNotNull();
        assertThat(response.getNodes().get(0).getState()).containsKey("@file");
        assertThat(get(response.getNodes().get(0).getState(), "@file", "document_signatures"))
                .isNotNull().isInstanceOf(List.class);

        // when
        ErrorResponse error = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(documentType)
                        .file(otherPdf))
                .addLinkRequest(0, node, "#anchor")));

        // then
        SlglAssertions.assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldCheckFirstElementLinkedToAnchor() {
        // given
        NodeResponse anchorType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .stateProperties("value")
                .permission(allowReadStateForEveryone()));

        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#anchor", anchorType)
                        .anchor("#test_first")
                        .anchor("#test_oldest")
                        .permission(Permission.builder()
                                .allow(allowLink("#anchor"))
                                .alwaysAllowed())
                        .permission(Permission.builder()
                                .allow(allowLink("#test_first"))
                                .requireAll(requireThat("$target_node.#anchor.$first.value").isEqualTo().value("valid")))
                        .permission(Permission.builder()
                                .allow(allowLink("#test_oldest"))
                                .requireAll(requireThat("$target_node.#anchor.$oldest.value").isEqualTo().value("valid")))
                ));

        ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(anchorType)
                        .data("value", "valid"))
                .addLinkRequest(0, node, "#anchor"));

        // when
        WriteResponse response1 = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#test_first"));
        WriteResponse response2 = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#test_oldest"));

        // then
        assertThat(response1).isNotNull();
        assertThat(response2).isNotNull();

        // given
        ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(anchorType)
                        .data("value", "not_valid"))
                .addLinkRequest(0, node, "#anchor"));

        // when
        WriteResponse response3 = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#test_first"));
        WriteResponse response4 = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#test_oldest"));

        // then
        assertThat(response3).isNotNull();
        assertThat(response4).isNotNull();
    }

    @Test
    public void shouldCheckLastElementLinkedToAnchor() {
        // given
        NodeResponse anchorType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .stateProperties("value")
                .permission(allowReadStateForEveryone()));

        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#anchor", anchorType)
                        .anchor("#test_last")
                        .anchor("#test_newest")
                        .permission(Permission.builder()
                                .allow(allowLink("#anchor"))
                                .alwaysAllowed())
                        .permission(Permission.builder()
                                .allow(allowLink("#test_last"))
                                .requireAll(requireThat("$target_node.#anchor.$last.value").isEqualTo().value("valid")))
                        .permission(Permission.builder()
                                .allow(allowLink("#test_newest"))
                                .requireAll(requireThat("$target_node.#anchor.$newest.value").isEqualTo().value("valid")))
                ));

        ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(anchorType)
                        .data("value", "valid"))
                .addLinkRequest(0, node, "#anchor"));

        // when
        WriteResponse response1 = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#test_last"));
        WriteResponse response2 = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#test_newest"));

        // then
        assertThat(response1).isNotNull();
        assertThat(response2).isNotNull();

        // given
        ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(anchorType)
                        .data("value", "not_valid"))
                .addLinkRequest(0, node, "#anchor"));

        // when
        ErrorResponse response3 = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#test_last")));
        ErrorResponse response4 = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#test_newest")));

        // then
        assertThat(response3).hasErrorCode(ErrorCode.PERMISSION_DENIED);
        assertThat(response4).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        // given
        ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(anchorType)
                        .data("value", "valid"))
                .addLinkRequest(0, node, "#anchor"));

        // when
        WriteResponse response5 = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#test_last"));
        WriteResponse response6 = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#test_newest"));

        // then
        assertThat(response5).isNotNull();
        assertThat(response6).isNotNull();
    }

    @Test
    public void shouldCheckInlineLinksWhenCheckingFirstAndLastElementLinkedToAnchor() {
        // given
        NodeResponse anchorType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .stateProperties("value")
                .permission(allowReadStateForEveryone()));

        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#anchor", anchorType)
                        .anchor("#test_first")
                        .anchor("#test_last")
                        .permission(Permission.builder()
                                .allow(allowLink("#anchor"))
                                .alwaysAllowed())
                        .permission(Permission.builder()
                                .allow(allowLink("#test_first"))
                                .requireAll(requireThat("$target_node.#anchor.$first.value").isEqualTo().value("valid")))
                        .permission(Permission.builder()
                                .allow(allowLink("#test_last"))
                                .requireAll(requireThat("$target_node.#anchor.$last.value").isEqualTo().value("valid"))))
                .linkNode("#anchor", NodeRequest.builder()
                        .type(anchorType)
                        .data("value", "valid"))
                .linkNode("#anchor", NodeRequest.builder()
                        .type(anchorType)
                        .data("value", "not_valid")));

        // when
        WriteResponse response1 = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#test_first"));
        ErrorResponse response2 = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#test_last")));

        // then
        assertThat(response1).isNotNull();
        assertThat(response2).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        // given
        ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(anchorType)
                        .data("value", "valid"))
                .addLinkRequest(0, node, "#anchor"));

        // when
        WriteResponse response3 = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#test_first"));
        WriteResponse response4 = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#test_last"));

        // then
        assertThat(response3).isNotNull();
        assertThat(response4).isNotNull();

        // given
        ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(anchorType)
                        .data("value", "not_valid"))
                .addLinkRequest(0, node, "#anchor"));

        // when
        WriteResponse response5 = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#test_first"));
        ErrorResponse response6 = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#test_last")));

        // then
        assertThat(response5).isNotNull();
        assertThat(response6).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    /*
    @Test
    public void shouldCheckThatNodeIsLinked() {
        // given
        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId("node"))
                .type(TypeNodeRequest.builder()
                        .anchor("#test")
                        .permission(Permission.builder()
                                .allow(allowLink("#test"))
                                .requireAll(requireThat("$target_node.$links.$length").isGreaterThan().value(0)))));

        // when
        ErrorResponse error = expectError(() -> ledger.write(WriteRequest.builder()
                .addNode(NodeRequest.builder())
                .addLink(0, node, "#test")));

        // then
        SlglAssertions.assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        // given
        ledger.write(WriteRequest.builder()
                .addNode(NodeRequest.builder()
                        .type(TypeNodeRequest.builder()
                                .anchor("#anchor")
                                .permission(allowAllForEveryone())))
                .addLink(node, 0, "#anchor"));

        // when
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addNode(NodeRequest.builder())
                .addLink(0, node, "#test"));

        // then
        assertThat(response).isNotNull();
    }

    @Test
    public void shouldCheckThatNodeIsInlineLinked() {
        // given
        NodeResponse type = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId("type"))
                .anchor("#test")
                .permission(Permission.builder()
                        .allow(allowLink("#test"))
                        .requireAll(requireThat("$target_node.$links.$length").isGreaterThan().value(0))));

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId("node"))
                .type(type)
                .linkNode("#test", NodeRequest.builder())));

        // then
        SlglAssertions.assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        // when
        NodeResponse response = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId("node"))
                .type(TypeNodeRequest.builder()
                        .anchor("#anchor"))
                .linkNode("#anchor", NodeRequest.builder()
                        .type(type)
                        .linkNode("#test", NodeRequest.builder())));

        // then
        assertThat(response).isNotNull();
    }

    @Test
    public void shouldCheckStateOfNodeToWhichNodeIsLinked() {
        // given
        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId("node"))
                .type(TypeNodeRequest.builder()
                        .anchor("#test")
                        .permission(Permission.builder()
                                .allow(allowLink("#test"))
                                .requireAll(requireThat("$target_node.$links.$last.value").isEqualTo().value("valid")))));

        ledger.write(WriteRequest.builder()
                .addNode(NodeRequest.builder()
                        .id(generateUniqueId("parent_node"))
                        .type(TypeNodeRequest.builder()
                                .stateProperties("value")
                                .anchor("#anchor")
                                .permissions(allowAllForEveryone()))
                        .data("value", "not_valid"))
                .addLink(node, 0, "#anchor"));

        // when
        ErrorResponse error = expectError(() -> ledger.write(WriteRequest.builder()
                .addNode(NodeRequest.builder())
                .addLink(0, node, "#test")));

        // then
        SlglAssertions.assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        // given
        ledger.write(WriteRequest.builder()
                .addNode(NodeRequest.builder()
                        .id(generateUniqueId("parent_node"))
                        .type(TypeNodeRequest.builder()
                                .stateProperties("value")
                                .anchor("#anchor")
                                .permissions(allowAllForEveryone()))
                        .data("value", "valid"))
                .addLink(node, 0, "#anchor"));

        // when
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addNode(NodeRequest.builder())
                .addLink(0, node, "#test"));

        // then
        assertThat(response).isNotNull();
    }

    @Test
    public void shouldCheckStateOfNodeToWhichNodeIsInlineLinked() {
        // given
        Function<String, NodeRequest> requestTemplate = (value) -> NodeRequest.builder()
                .id(generateUniqueId("node"))
                .type(TypeNodeRequest.builder()
                        .stateProperties("value")
                        .anchor("#anchor")
                        .permissions(allowAllForEveryone()))
                .data("value", value)
                .linkNode("#anchor", NodeRequest.builder()
                        .type(TypeNodeRequest.builder()
                                .anchor("#test")
                                .permission(Permission.builder()
                                        .allow(allowLink("#test"))
                                        .requireAll(requireThat("$target_node.$links.$last.value").isEqualTo().value("valid"))))
                        .linkNode("#test", NodeRequest.builder()))
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(requestTemplate.apply("not_valid")));

        // then
        SlglAssertions.assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        // when
        NodeResponse response = ledger.writeNode(requestTemplate.apply("valid"));

        // then
        assertThat(response).isNotNull();
    }

    @Test
    public void shouldCheckAnchorToWhichNodeThatNodeIsLinked() {
    }

    @Test
    public void shouldCheckAnchorToWhichNodeThatNodeIsInlineLinked() {
    }
    */
}
