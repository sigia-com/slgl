package io.slgl.api.it;

import io.slgl.api.utils.ErrorCode;
import io.slgl.client.error.ErrorResponse;
import io.slgl.client.node.*;
import io.slgl.client.node.permission.Permission;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.it.data.PermissionMother.*;
import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;
import static io.slgl.client.node.permission.Allow.allowLink;
import static io.slgl.client.node.permission.Requirement.requireThat;

public class UnlinkIT extends AbstractApiTest {

    @Test
    public void shouldUnlink() {
        // given
        NodeResponse node = user.writeNode(NodeRequest.builder()
                .type(TypeNodeRequest.builder()
                        .anchor("#anchor")
                        .permission(allowLinkForEveryone("#anchor"))
                        .permission(allowUnlinkForEveryone("#anchor"))
                        .anchor("#test")
                        .permission(Permission.builder()
                                .allow(allowLink("#test"))
                                .requireAll(requireThat("$target_node.#anchor.$length").isEqualTo().value(0)))));

        WriteResponse response = user.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#anchor"));

        ErrorResponse testError = expectError(() -> user.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#test")));
        assertThat(testError).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        // when
        user.write(WriteRequest.builder()
                .addUnlinkRequest(response.getLinks().get(0).getId()));

        // then
        WriteResponse testResponse = user.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#test"));

        assertThat(testResponse).isNotNull();
    }

    @Test
    public void shouldUpdateFirstIndexWhenUnlinking() {
        // given
        NodeResponse typeWithValue = user.writeNode(TypeNodeRequest.builder()
                .stateProperties("value")
                .permission(allowReadStateForEveryone()));

        NodeResponse node = user.writeNode(NodeRequest.builder()
                .type(TypeNodeRequest.builder()
                        .anchor("#anchor")
                        .permission(allowLinkForEveryone("#anchor"))
                        .permission(allowUnlinkForEveryone("#anchor"))
                        .anchor("#test")
                        .permission(Permission.builder()
                                .allow(allowLink("#test"))
                                .requireAll(requireThat("$target_node.#anchor.$first.value").isEqualTo().ref("$source_node.value")))));

        WriteResponse linkResponse = user.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder().type(typeWithValue).data("value", "a"))
                .addRequest(NodeRequest.builder().type(typeWithValue).data("value", "b"))
                .addRequest(NodeRequest.builder().type(typeWithValue).data("value", "c"))
                .addLinkRequest(0, node, "#anchor")
                .addLinkRequest(1, node, "#anchor")
                .addLinkRequest(2, node, "#anchor"));

        WriteRequest testRequest = WriteRequest.builder()
                .addRequest(NodeRequest.builder().type(typeWithValue).data("value", "c"))
                .addLinkRequest(0, node, "#test")
                .build();

        ErrorResponse testError = expectError(() -> user.write(testRequest));
        assertThat(testError).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        // when
        user.write(WriteRequest.builder()
                .addUnlinkRequest(linkResponse.getLinks().get(1).getId()));
        user.write(WriteRequest.builder()
                .addUnlinkRequest(linkResponse.getLinks().get(0).getId()));

        // then
        WriteResponse testResponse = user.write(testRequest);
        assertThat(testResponse).isNotNull();
    }

    @Test
    public void shouldUpdateLastIndexWhenUnlinking() {
        // given
        NodeResponse typeWithValue = user.writeNode(TypeNodeRequest.builder()
                .stateProperties("value")
                .permission(allowReadStateForEveryone()));

        NodeResponse node = user.writeNode(NodeRequest.builder()
                .type(TypeNodeRequest.builder()
                        .anchor("#anchor")
                        .permission(allowLinkForEveryone("#anchor"))
                        .permission(allowUnlinkForEveryone("#anchor"))
                        .anchor("#test")
                        .permission(Permission.builder()
                                .allow(allowLink("#test"))
                                .requireAll(requireThat("$target_node.#anchor.$last.value").isEqualTo().ref("$source_node.value")))));

        WriteResponse linkResponse = user.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder().type(typeWithValue).data("value", "a"))
                .addRequest(NodeRequest.builder().type(typeWithValue).data("value", "b"))
                .addRequest(NodeRequest.builder().type(typeWithValue).data("value", "c"))
                .addLinkRequest(0, node, "#anchor")
                .addLinkRequest(1, node, "#anchor")
                .addLinkRequest(2, node, "#anchor"));

        WriteRequest testRequest = WriteRequest.builder()
                .addRequest(NodeRequest.builder().type(typeWithValue).data("value", "a"))
                .addLinkRequest(0, node, "#test")
                .build();

        ErrorResponse testError = expectError(() -> user.write(testRequest));
        assertThat(testError).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        // when
        user.write(WriteRequest.builder()
                .addUnlinkRequest(linkResponse.getLinks().get(1).getId()));
        user.write(WriteRequest.builder()
                .addUnlinkRequest(linkResponse.getLinks().get(2).getId()));

        // then
        WriteResponse testResponse = user.write(testRequest);
        assertThat(testResponse).isNotNull();
    }

    @Test
    public void shouldUnlinkInBatch() {
        shouldUnlinkInBatch(10);
    }

    @Test
    public void shouldReturnErrorWhenCreatingTooManyUnlinksInSingleBatch() {
        // when
        ErrorResponse error = expectError(() -> shouldUnlinkInBatch(11));

        // then
        assertThat(error).hasErrorCodeAndMessage(ErrorCode.TOO_MANY_REQUESTS_IN_BATCH, 20);
    }

    private void shouldUnlinkInBatch(int numberOfUnlinks) {
        // given

        NodeResponse rootNode = user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#anchor")
                        .permission(allowAllForEveryone())));

        List<LinkResponse> links = new ArrayList<>();
        int numberOfLinks = numberOfUnlinks + 2; // we don't want to delete first and last link, because it is simpler (less QLDB updates)
        while (links.size() < numberOfLinks) {
            int batchSize = Math.min(numberOfLinks - links.size(), 10);

            WriteRequest.Builder requestBuilder = WriteRequest.builder();
            for (int i = 0; i < batchSize; i++) {
                requestBuilder.addRequest(NodeRequest.builder());
                requestBuilder.addLinkRequest(i * 2, rootNode, "#anchor");
            }

            WriteResponse response = user.write(requestBuilder);
            links.addAll(response.getLinks());
        }

        WriteRequest.Builder unlinkRequestBuilder = WriteRequest.builder();
        for (int i = 0; i < numberOfUnlinks; i++) {
            unlinkRequestBuilder.addUnlinkRequest(links.get(i + 1).getId());
        }
        WriteRequest unlinkRequest = unlinkRequestBuilder.build();

        // when
        WriteResponse linksResponse = ledger.write(unlinkRequest);

        // then
        assertThat(linksResponse).isNotNull();
    }
}
