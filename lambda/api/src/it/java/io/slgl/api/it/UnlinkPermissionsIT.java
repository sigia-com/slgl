package io.slgl.api.it;

import io.slgl.api.utils.ErrorCode;
import io.slgl.client.error.ErrorResponse;
import io.slgl.client.node.*;
import io.slgl.client.node.permission.Permission;
import org.junit.jupiter.api.Test;

import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.it.data.PermissionMother.allowLinkForEveryone;
import static io.slgl.api.it.data.PermissionMother.allowReadStateForEveryone;
import static io.slgl.client.node.permission.Allow.allowUnlink;
import static io.slgl.client.node.permission.Requirement.requireThat;

public class UnlinkPermissionsIT extends AbstractApiTest {

    @Test
    public void shouldCheckPermissionsWhenUnlinking() {
        // given
        NodeResponse typeWithValue = user.writeNode(TypeNodeRequest.builder()
                .stateProperties("value")
                .permission(allowReadStateForEveryone()));

        NodeResponse typeWithUnlinkPermission = user.writeNode(TypeNodeRequest.builder()
                .stateProperties("value")
                .permission(allowReadStateForEveryone())
                .anchor("#anchor")
                .permission(allowLinkForEveryone("#anchor"))
                .permission(Permission.builder()
                        .allow(allowUnlink("#anchor"))
                        .requireAll(
                                requireThat("$target_node.value").isEqualTo().value("target_allow"),
                                requireThat("$source_node.value").isEqualTo().value("source_allow"))));

        WriteResponse allowResponse = user.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(typeWithUnlinkPermission)
                        .data("value", "target_allow"))
                .addRequest(NodeRequest.builder()
                        .type(typeWithValue)
                        .data("value", "source_allow"))
                .addLinkRequest(1, 0, "#anchor"));

        // when
        WriteResponse response = user.write(WriteRequest.builder()
                .addUnlinkRequest(allowResponse.getLinks().get(0).getId()));

        // then
        assertThat(response).isNotNull();

        // given
        WriteResponse disallowResponse = user.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(typeWithUnlinkPermission)
                        .data("value", "disallow"))
                .addRequest(NodeRequest.builder()
                        .type(typeWithValue)
                        .data("value", "disallow"))
                .addLinkRequest(1, 0, "#anchor"));

        // when
        ErrorResponse error = expectError(() -> user.write(WriteRequest.builder()
                .addUnlinkRequest(disallowResponse.getLinks().get(0).getId())));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldReturnErrorWhenUnlinkingWhenNoPermissionsArePresent() {
        // given
        WriteResponse response = user.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(TypeNodeRequest.builder()
                                .anchor("#anchor")
                                .permission(allowLinkForEveryone("#anchor"))))
                .addRequest(NodeRequest.builder())
                .addLinkRequest(1, 0, "#anchor"));


        // when
        ErrorResponse error = expectError(() -> user.write(WriteRequest.builder()
                .addUnlinkRequest(response.getLinks().get(0).getId())));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }
}
