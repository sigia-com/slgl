package io.slgl.api.it;

import com.google.common.collect.ImmutableMap;
import io.slgl.api.it.user.StateStorage;
import io.slgl.api.utils.ErrorCode;
import io.slgl.client.error.ErrorResponse;
import io.slgl.client.node.*;
import io.slgl.client.node.permission.Allow;
import io.slgl.client.node.permission.Permission;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.it.data.PermissionMother.allowReadStateForEveryone;
import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;
import static io.slgl.client.node.permission.Requirement.requireThat;

public class StateIT extends AbstractApiTest {

    @Test
    public void shouldUseStateProvidedInRequest() {
        // given
        NodeResponse baseNode = user.writeNode(NodeRequest.builder()
                .type(TypeNodeRequest.builder()
                        .stateProperties("expected_value")
                        .anchor("#test")
                        .permission(Permission.builder()
                                .allow(Allow.allowLink("#test"))
                                .requireAll(
                                        requireThat("$source_node.linked_value").isEqualTo().ref("$target_node.expected_value")
                                )))
                .data("expected_value", 42));

        NodeResponse testNode = user.writeNode(NodeRequest.builder()
                .type(TypeNodeRequest.builder()
                        .stateProperties("linked_value"))
                .data("linked_value", 42));

        // when
        ErrorResponse error = expectError(() -> user.write(WriteRequest.builder()
                .addLinkRequest(testNode, baseNode, "#test")));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        // when
        WriteResponse response = user.write(WriteRequest.builder()
                .addLinkRequest(testNode, baseNode, "#test")
                .addExistingNodeState(baseNode.getId(), baseNode.getState())
                .addExistingNodeState(testNode.getId(), testNode.getState()));

        // then
        assertThat(response).isNotNull();
    }

    @Test
    public void shouldReturnErrorWhenStateProvidedInRequestDoesNotMatchStateHash() {
        // given
        NodeResponse baseType = user.writeNode(TypeNodeRequest.builder()
                .stateProperties("expected_value")
                .anchor("#test")
                .permission(Permission.builder()
                        .allow(Allow.allowLink("#test"))
                        .requireAll(
                                requireThat("$source_node.linked_value").isEqualTo().ref("$target_node.expected_value")
                        )));

        NodeResponse baseNode = user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(baseType)
                .data("expected_value", 42));

        NodeResponse testType = user.writeNode(TypeNodeRequest.builder()
                .stateProperties("linked_value"));

        NodeResponse testNode = user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(testType)
                .data("linked_value", "some_not_expected_value"));

        Map<String, Integer> spoofedState = ImmutableMap.of("linked_value", 42);

        // when
        ErrorResponse error = expectError(() -> user.write(WriteRequest.builder()
                .addLinkRequest(testNode, baseNode, "#test")
                .addExistingNodeState(baseNode.getId(), baseNode.getState())
                .addExistingNodeState(testNode.getId(), spoofedState)));

        // then
        assertThat(error).hasErrorCodeAndMessage(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldReturnErrorWhenStateProvidedInStateSourceDoesNotMatchStateHash() {
        // given
        String testNodeId = generateUniqueId();
        String stateSource = StateStorage.INSTANCE.generateUniqueStateSource();

        StateStorage.ignoreStateStorageForNode(testNodeId);

        NodeResponse baseNode = user.writeNode(NodeRequest.builder()
                .type(TypeNodeRequest.builder()
                        .stateProperties("expected_value")
                        .anchor("#test")
                        .permission(Permission.builder()
                                .allow(Allow.allowLink("#test"))
                                .requireAll(
                                        requireThat("$source_node.linked_value").isEqualTo().ref("$target_node.expected_value")
                                ))
                        .permissions(allowReadStateForEveryone()))
                .data("expected_value", 42));

        NodeResponse testNode = user.writeNode(NodeRequest.builder()
                .id(testNodeId)
                .stateSource(stateSource)
                .type(TypeNodeRequest.builder()
                        .stateProperties("linked_value"))
                .data("linked_value", "some_not_expected_value"));

        Map<String, Integer> spoofedState = ImmutableMap.of("linked_value", 42);
        StateStorage.INSTANCE.storeState(testNode, spoofedState);

        // when
        ErrorResponse error = expectError(() -> user.write(WriteRequest.builder()
                .addLinkRequest(testNode, baseNode, "#test")));

        // then
        assertThat(error).hasErrorCodeAndMessage(ErrorCode.PERMISSION_DENIED);
    }
}
