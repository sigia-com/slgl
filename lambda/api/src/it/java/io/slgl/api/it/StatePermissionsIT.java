package io.slgl.api.it;

import io.slgl.api.it.user.User;
import io.slgl.api.utils.ErrorCode;
import io.slgl.client.node.*;
import io.slgl.client.node.permission.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;
import static io.slgl.client.node.permission.Allow.allowLink;
import static io.slgl.client.node.permission.Allow.allowReadState;
import static io.slgl.client.node.permission.Requirement.requireThat;

public class StatePermissionsIT extends AbstractApiTest {

    private User otherUser;

    @BeforeEach
    public void init() {
        otherUser = getSecondUser();
    }

    @Test
    public void shouldReadStateWhenReadStatePermissionIsPresent() {
        // given
        var type = createTypeWithStateProperty(createReadStatePermissionForUser(otherUser));
        var object = createObjectWithState(type, "expected-value");

        // when
        var entryResponse = otherUser.readNode(object.getId(), ReadState.WITH_STATE);

        // then
        assertThat(entryResponse.getState()).containsEntry("value", "expected-value");
    }

    @Test
    public void shouldReturnErrorWhenReadStatePermissionInNotPresent() {
        // given
        var type = createTypeWithStateProperty(null);
        var object = createObjectWithState(type, "expected-value");

        // when
        var error = expectError(() -> otherUser.readNode(object.getId(), ReadState.WITH_STATE));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldNotReturnErrorWhenReadStatePermissionInNotPresentButReadWasExecutedWithDoNotFailOnUnauthorized() {
        // given
        var type = createTypeWithStateProperty(null);
        var object = createObjectWithState(type, "expected-value");

        // when
        var entryResponse = otherUser.readNode(object.getId(), ReadState.WITH_STATE_IF_AUTHORIZED);

        // then
        assertThat(entryResponse.getState()).isNull();
    }

    @Test
    public void shouldHandleReadStatePermissionThatAccessState() {
        // given
        NodeRequest.Builder<?> request = NodeRequest.builder()
                .type(TypeNodeRequest.builder()
                        .stateProperties("value")
                        .anchor("#child")
                        .permission(Permission.builder()
                                .allow(allowReadState())
                                .requireAll(
                                        requireThat("$principals").atLeastOne().meetsAllRequirements(
                                                requireThat("$current.api.username").isEqualTo().value(user.getUsername())
                                        )
                                ))
                        .permission(Permission.builder()
                                .allow(allowReadState())
                                .requireAll(
                                        requireThat("$node.value").isEqualTo().value("allow")
                                )
                                .evaluateStateAccessAsUser(user.getUsername())));

        NodeResponse entryWithAllow = user.writeNode(request
                .id(generateUniqueId())
                .data("value", "allow")
                .build());

        NodeResponse entryWithDeny = user.writeNode(request
                .id(generateUniqueId())
                .data("value", "deny")
                .build());

        // when
        NodeResponse readResponse = otherUser.readNode(entryWithAllow.getId(), ReadState.WITH_STATE);

        // then
        assertThat(readResponse).isNotNull();

        // when
        var error = expectError(() -> otherUser.readNode(entryWithDeny.getId(), ReadState.WITH_STATE));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldHandleReadStatePermissionThatChecksLinkedNodes() {
        // given
        NodeResponse node = user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .stateProperties("value")
                        .anchor("#allow")
                        .permission(Permission.builder()
                                .allow(allowLink("#allow"))
                                .alwaysAllowed())
                        .permission(Permission.builder()
                                .allow(allowReadState())
                                .requireAll(
                                        requireThat("$node.#allow.$length").isEqualTo().value(1)
                                )))
                .data("value", "foo")
                .build());

        // when
        var error = expectError(() -> user.readNode(node.getId(), ReadState.WITH_STATE));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        // given
        user.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#allow"));

        // when
        NodeResponse readResponse = user.readNode(node.getId(), ReadState.WITH_STATE);

        // then
        assertThat(readResponse).isNotNull();
    }


    @Test
    public void shouldReturnErrorWhenReadStatePermissionHasInfiniteLoop() {
        // given
        NodeResponse entry = user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .permission(Permission.builder()
                                .allow(allowReadState())
                                .requireAll(
                                        requireThat("$node.value").isEqualTo().value(1)
                                ))));

        // when
        var error = expectError(() -> user.readNode(entry.getId(), ReadState.WITH_STATE));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    private NodeResponse createTypeWithStateProperty(Permission permission) {
        TypeNodeRequest.Builder request = TypeNodeRequest.builder()
                .id(generateUniqueId())
                .stateProperties("value")
                .permission(permission);

        return user.writeNode(request.build());
    }

    private Permission createReadStatePermissionForUser(User userForPermission) {
        return Permission.builder()
                .allow(allowReadState())
                .requireAll(
                        requireThat("$principals").atLeastOne().meetsAllRequirements(
                                requireThat("$current.api.username").isEqualTo().value(userForPermission.getUsername())
                        )
                )
                .build();
    }

    private NodeResponse createObjectWithState(NodeResponse type, String value) {
        NodeRequest.Builder<?> request = NodeRequest.builder()
                .id(generateUniqueId())
                .type(type)
                .data("value", value);

        return user.writeNode(request.build());
    }
}
