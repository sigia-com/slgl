package io.slgl.api.it;

import io.slgl.api.utils.ErrorCode;
import io.slgl.client.Types;
import io.slgl.client.camouflage.Camouflage;
import io.slgl.client.error.ErrorResponse;
import io.slgl.client.node.*;
import io.slgl.client.node.permission.Permission;
import org.junit.jupiter.api.Test;

import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.it.data.PermissionMother.allowAllForEveryone;
import static io.slgl.api.it.data.PermissionMother.allowReadStateForEveryone;
import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;
import static io.slgl.client.node.NodeTypeId.simple;
import static io.slgl.client.node.permission.Allow.allowLink;
import static io.slgl.client.node.permission.Requirement.requireThat;

public class CamouflageLinkIT extends AbstractApiTest {

    @Test
    public void shouldLinkToNodeWithCamouflageAndInlineType() {
        // given
        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#a")
                        .permission(allowAllForEveryone()))
                .camouflage(buildCamouflage("#a")));

        // when
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#a"));

        // then
        assertThat(response.getLinks().get(0).getTargetNode()).isEqualTo(node.getId());
        assertThat(response.getLinks().get(0).getTargetAnchor()).isEqualTo("#aC");
    }

    @Test
    public void shouldLinkToNodeWithCamouflageAndStandaloneType() {
        // given
        NodeResponse type = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#a")
                .permission(allowAllForEveryone()));

        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(type)
                .camouflage(buildCamouflage("#a")));

        // when
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#a"));

        // then
        assertThat(response.getLinks().get(0).getTargetNode()).isEqualTo(node.getId());
        assertThat(response.getLinks().get(0).getTargetAnchor()).isEqualTo("#aC");
    }

    @Test
    public void shouldLinkToFakeAnchor() {
        // given
        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .permission(Permission.builder()
                                .allow(allowLink("#fake"))
                                .requireAll(
                                        requireThat("$principals").atLeastOne().meetsAllRequirements(
                                                requireThat("$current.api.username").isEqualTo().value(user.getUsername())
                                        )
                                )))
                .camouflage(buildCamouflageWithFakeAnchors("#fake1", "#fake2", "#fake3")));

        // when
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#fake1"));

        // then
        assertThat(response.getLinks().get(0).getTargetNode()).isEqualTo(node.getId());
        assertThat(response.getLinks().get(0).getTargetAnchor()).isEqualTo("#fake1");
    }

    @Test
    public void shouldNotLinkToFakeAnchorWithoutPermissions() {
        // given
        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .camouflage(buildCamouflageWithFakeAnchors("#fake1", "#fake2", "#fake3")));

        // when
        ErrorResponse errorResponse = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#fake1")));

        // then
        assertThat(errorResponse).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldResolveRealAnchorsInCamouflagedNodeWhenCheckingPermissionsInInlineType() {
        // given
        NodeResponse typeA = ledger.writeNode(TypeNodeRequest.builder()
                .stateProperties("valueA")
                .permission(allowReadStateForEveryone()));

        NodeResponse typeB = ledger.writeNode(TypeNodeRequest.builder()
                .stateProperties("valueB")
                .permission(allowReadStateForEveryone()));

        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#a", typeA)
                        .anchor("#b", typeB)
                        .permission(Permission.builder()
                                .allow(allowLink("#a"))
                                .alwaysAllowed())
                        .permission(Permission.builder()
                                .allow(allowLink("#b"))
                                .requireAll(
                                        requireThat("$target_node.#a.valueA").sum().isGreaterThan().ref("$source_node.valueB")
                                )))
                .camouflage(buildCamouflage("#a", "#b")));

        ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(typeA)
                        .data("valueA", 5))
                .addLinkRequest(0, node, "#a"));

        // when
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(typeB)
                        .data("valueB", 4))
                .addLinkRequest(0, node, "#b"));

        // then
        assertThat(response).isNotNull();

        // when
        ErrorResponse error = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(typeB)
                        .data("valueB", 100))
                .addLinkRequest(0, node, "#b")));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldResolveRealAnchorsInCamouflagedNodeWhenCheckingPermissionsInStandaloneType() {
        // given
        NodeResponse typeA = ledger.writeNode(TypeNodeRequest.builder()
                .stateProperties("valueA")
                .permission(allowReadStateForEveryone()));

        NodeResponse typeB = ledger.writeNode(TypeNodeRequest.builder()
                .stateProperties("valueB")
                .permission(allowReadStateForEveryone()));

        NodeResponse type = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#a", typeA)
                .anchor("#b", typeB)
                .permission(Permission.builder()
                        .allow(allowLink("#a"))
                        .alwaysAllowed())
                .permission(Permission.builder()
                        .allow(allowLink("#b"))
                        .requireAll(
                                requireThat("$target_node.#a.valueA").sum().isGreaterThan().ref("$source_node.valueB")
                        )));

        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(type)
                .camouflage(buildCamouflage("#a", "#b")));

        ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(typeA)
                        .data("valueA", 5))
                .addLinkRequest(0, node, "#a"));

        // when
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(typeB)
                        .data("valueB", 4))
                .addLinkRequest(0, node, "#b"));

        // then
        assertThat(response).isNotNull();

        // when
        ErrorResponse error = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(typeB)
                        .data("valueB", 100))
                .addLinkRequest(0, node, "#b")));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldLinkCamouflagedNodeToCamouflagedNode() {
        // given
        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#a", TypeNodeRequest.builder().stateProperties("valueA"))
                        .anchor("#b", TypeNodeRequest.builder().stateProperties("valueB"))
                        .permission(allowAllForEveryone()))
                .camouflage(buildCamouflage("#a", "#b")));

        // when
        WriteResponse linkA = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(TypeNodeRequest.builder()
                                .stateProperties("valueA")
                                .anchor("#c")
                                .permission(allowAllForEveryone()))
                        .camouflage(Camouflage.builder()
                                .anchor("#observersC", "#observers")
                                .anchor("#auditorsC", "#auditors")
                                .anchor("#cC", "#c")
                                .fakeAnchors("#fakeX1", "#fakeX2")
                        ))
                .addLinkRequest(0, node, "#a"));

        WriteResponse linkC = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .camouflage(Camouflage.builder()
                                .anchor("#observersC", "#observers")
                                .anchor("#auditorsC", "#auditors")
                                .fakeAnchors("#fakeY1", "#fakeY2")
                        ))
                .addLinkRequest(0, linkA.getNodes().get(0), "#c"));

        // then
        assertThat(node.getType()).isEqualTo(simple(Types.CAMOUFLAGE));

        assertThat(linkA.getNodes().get(0).getType()).isEqualTo(simple(Types.CAMOUFLAGE));
        assertThat(linkA.getLinks().get(0).getTargetNode()).isEqualTo(node.getId());
        assertThat(linkA.getLinks().get(0).getTargetAnchor()).isEqualTo("#aC");

        assertThat(linkC.getNodes().get(0).getType()).isEqualTo(simple(Types.CAMOUFLAGE));
        assertThat(linkC.getLinks().get(0).getTargetNode()).isEqualTo(linkA.getNodes().get(0).getId());
        assertThat(linkC.getLinks().get(0).getTargetAnchor()).isEqualTo("#cC");
    }

    @Test
    public void shouldReturnPermissionDeniedWhenLinkingToNonExistingAnchorInCamouflagedNode() {
        // given
        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .camouflage(buildCamouflage()));

        // when
        ErrorResponse error = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#not_existing_anchor")));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }


    private static Camouflage buildCamouflage(String... anchors) {
        Camouflage.Builder camouflage = Camouflage.builder()
                .anchor("#observersC", "#observers")
                .anchor("#auditorsC", "#auditors");

        for (String anchor : anchors) {
            camouflage.anchor(anchor + "C", anchor);
        }

        return camouflage.build();
    }

    private static Camouflage buildCamouflageWithFakeAnchors(String... fakeAnchors) {
        return Camouflage.builder()
                .anchor("#observersC", "#observers")
                .anchor("#auditorsC", "#auditors")
                .fakeAnchors(fakeAnchors)
                .build();
    }
}

