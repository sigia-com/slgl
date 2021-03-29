package io.slgl.api.it;

import io.slgl.api.utils.ErrorCode;
import io.slgl.client.camouflage.Camouflage;
import io.slgl.client.error.ErrorResponse;
import io.slgl.client.node.*;
import io.slgl.client.node.permission.Permission;
import org.junit.jupiter.api.Test;

import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.it.data.PermissionMother.allowAllForEveryone;
import static io.slgl.api.it.data.PermissionMother.allowReadStateForEveryone;
import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;
import static io.slgl.client.node.permission.Allow.allowLink;
import static io.slgl.client.node.permission.Requirement.requireThat;

public class LinkPermissionsIT extends AbstractApiTest {

    @Test
    public void shouldCheckPermissionsWhenLinking() {
        // given
        NodeResponse type = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#test")
                .permission(Permission.builder()
                        .allow(allowLink("#test"))
                        .requireAll(
                                requireThat("$source_node.value").isEqualTo().value("valid")
                        )));

        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(type));

        NodeResponse childType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .stateProperties("value")
                .permission(allowReadStateForEveryone()));

        // when
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(childType)
                        .data("value", "valid"))
                .addLinkRequest(0, node, "#test"));

        // then
        assertThat(response).isNotNull();

        // when
        ErrorResponse error = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(childType)
                        .data("value", "not_valid"))
                .addLinkRequest(0, node, "#test")));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldCheckPermissionsWhenInlineLinking() {
        // given
        NodeResponse type = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#test")
                .permission(Permission.builder()
                        .allow(allowLink("#test"))
                        .requireAll(
                                requireThat("$source_node.value").isEqualTo().value("valid")
                        )));

        // when
        NodeResponse response = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(type)
                .linkNode("#test", NodeRequest.builder()
                        .data("value", "valid")));

        // then
        assertThat(response).isNotNull();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(type)
                .linkNode("#test", NodeRequest.builder()
                        .data("value", "not_valid"))));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldCheckPermissionsWhenNestedInlineLinking() {
        // given
        NodeResponse type = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#anchor")
                .permission(allowAllForEveryone()));

        NodeResponse nestedType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#test")
                .permission(Permission.builder()
                        .allow(allowLink("#test"))
                        .requireAll(
                                requireThat("$source_node.value").isEqualTo().value("valid")
                        )));

        // when
        NodeResponse response = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(type)
                .linkNode("#anchor", NodeRequest.builder()
                        .type(nestedType)
                        .linkNode("#test", NodeRequest.builder()
                                .data("value", "valid"))));

        // then
        assertThat(response).isNotNull();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(type)
                .linkNode("#anchor", NodeRequest.builder()
                        .type(nestedType)
                        .linkNode("#test", NodeRequest.builder()
                                .data("value", "not_valid")))));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldGivePermissionsToLinkToSpecificAnchor() {
        // given
        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#allow")
                        .anchor("#deny")
                        .permission(Permission.builder()
                                .allow(allowLink("#allow"))
                                .requireLogic(true))
                        .permission(Permission.builder()
                                .allow(allowLink("#deny"))
                                .requireLogic(false))));

        // when
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#allow"));

        // then
        assertThat(response).isNotNull();

        // when
        ErrorResponse error = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#deny")));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldReturnErrorWhenLinkingWhenNoPermissionsArePresent() {
        // given
        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#anchor")));

        // when
        ErrorResponse error = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#anchor")));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldAllowToInlineLinkWhenNoPermissionsArePresent() {
        // given
        NodeRequest nodeWithInlineLinkRequest = NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#anchor"))
                .linkNode("#anchor", NodeRequest.builder().build())
                .build();

        // when
        NodeResponse response = ledger.writeNode(nodeWithInlineLinkRequest);

        // then
        assertThat(response).isNotNull();
    }

    @Test
    public void shouldGivePermissionsToLinkToSpecificAnchorOnCamouflagedNode() {
        // given
        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#allow")
                        .anchor("#deny")
                        .permission(Permission.builder()
                                .allow(allowLink("#allow"))
                                .requireLogic(true))
                        .permission(Permission.builder()
                                .allow(allowLink("#deny"))
                                .requireLogic(false)))
                .camouflage(Camouflage.builder()
                        .anchor("#observers_camouflaged", "#observers")
                        .anchor("#auditors_camouflaged", "#auditors")
                        .anchor("#allow_camouflaged", "#allow")
                        .anchor("#deny_camouflaged", "#deny")
                )
        );

        // when
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#allow"));

        // then
        assertThat(response).isNotNull();
        assertThat(response.getLinks().get(0).getTargetNode()).isEqualTo(node.getId());
        assertThat(response.getLinks().get(0).getTargetAnchor()).isEqualTo("#allow_camouflaged");

        // when
        ErrorResponse error = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#deny")));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }
}
