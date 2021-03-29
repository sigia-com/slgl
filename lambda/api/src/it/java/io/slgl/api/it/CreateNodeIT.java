package io.slgl.api.it;

import io.slgl.api.it.user.StateStorage;
import io.slgl.api.it.utils.HashUtils;
import io.slgl.api.utils.ErrorCode;
import io.slgl.client.error.ErrorResponse;
import io.slgl.client.node.*;
import org.junit.jupiter.api.Test;

import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.it.data.PermissionMother.allowAllForEveryone;
import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

public class CreateNodeIT extends AbstractApiTest {

    @Test
    public void shouldCreateNode() {
        // given
        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .data("example_key", "example_value")
                .build();

        // when
        NodeResponse response = ledger.writeNode(request);

        // then
        assertThat(response.getId()).isEqualTo(request.getId());
        assertThat(response.getCreated()).isNotBlank();
        assertThat(response.getObjectSha3()).isNotBlank().hasSize(512 / 4);
    }

    @Test
    public void shouldFailWhenAddingNodeWithDuplicatedId() {
        // given
        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .data("example_key", "example_value")
                .build();

        ledger.writeNode(request);

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(error).hasErrorCode(ErrorCode.NODE_ALREADY_EXISTS);
    }

    @Test
    public void shouldGenerateExpectedObjectHash() {
        // given
        String id = generateUniqueId();
        StateStorage.ignoreStateStorageForNode(id);

        NodeRequest request = NodeRequest.builder().id(id)
                .data("second_key", "second_value")
                .data("first_key", "first_value")
                .build();

        // when
        NodeResponse response = ledger.writeNode(request);

        // then
        String expectedHash = HashUtils.sha3_512("{\n" +
                "  \"@id\": \"" + id + "\",\n" +
                "  \"first_key\": \"first_value\",\n" +
                "  \"second_key\": \"second_value\"\n" +
                "}\n");

        assertThat(response.getObjectSha3()).isEqualTo(expectedHash);
    }

    @Test
    public void shouldAutogenerateIdWhenWritingNodeWithoutId() {
        // given
        NodeRequest request = NodeRequest.builder()
                .id(null)
                .data("example_key", "example_value")
                .build();

        // when
        NodeResponse response = ledger.writeNode(request);

        // then
        assertThat(response.getId()).isNotEmpty();
    }

    @Test
    public void shouldCreateConnectedNodesInBatch() {
        // given
        TypeNodeRequest typeRequest = TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#child")
                .permission(allowAllForEveryone())
                .build();

        NodeRequest nodeRequest = NodeRequest.builder()
                .id(generateUniqueId())
                .type(typeRequest.getId())
                .build();

        WriteRequest request = WriteRequest.builder()
                .addRequest(typeRequest)
                .addRequest(nodeRequest)
                .addLinkRequest(typeRequest.getId(), nodeRequest.getId(), "#child")
                .build();

        // when
        WriteResponse response = ledger.write(request);

        // then
        assertThat(response.getNodes()).hasSize(2);
    }

    @Test
    public void shouldCreateNodesInBatch() {
        shouldCreateNodesInBatch(20);
    }

    @Test
    public void shouldReturnErrorWhenCreatingTooManyNodesInSingleBatch() {
        // when
        ErrorResponse error = expectError(() -> shouldCreateNodesInBatch(21));

        // then
        assertThat(error).hasErrorCodeAndMessage(ErrorCode.TOO_MANY_REQUESTS_IN_BATCH, 20);
    }

    private void shouldCreateNodesInBatch(int numberOfNodes) {
        // given
        WriteRequest.Builder requestBuilder = WriteRequest.builder();
        for (int i = 0; i < numberOfNodes; i++) {
            requestBuilder.addRequest(NodeRequest.builder()
                    .id(generateUniqueId())
                    .data("example_key_" + i, "example_value_" + i));
        }
        WriteRequest request = requestBuilder.build();

        // when
        WriteResponse response = ledger.write(request);

        // then
        assertThat(response.getResponses()).zipSatisfy(request.getRequests(),
                (responseItem, requestItem) -> {
                    assertThat(responseItem)
                            .asInstanceOf(type(NodeResponse.class))
                            .extracting(NodeResponse::getId).isEqualTo(((NodeRequest) requestItem).getId());
                });
    }

    @Test
    public void shouldReturnErrorWhenCreatingNodeWithIdThatIsNotUrl() {
        // given
        NodeRequest request = NodeRequest.builder()
                .id("not_a_valid_uuid")
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(error)
                .hasErrorCode(ErrorCode.VALIDATION_ERROR)
                .hasOnlyFieldError("requests[0].@id", "pattern");
    }

    @Test
    public void shouldReturnErrorWhenCreatingNodeWithIdThatContainsAnchor() {
        // given
        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId() + "#anchor")
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(error).hasErrorCode(ErrorCode.VALIDATION_ERROR)
                .hasOnlyFieldError("requests[0].@id", "pattern");
    }

    @Test
    public void shouldReturnErrorWhenCreatingNodeWithReservedField() {
        // given
        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .data("@reserved_field", "some-value")
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(error).hasErrorCodeAndMessage(ErrorCode.UNABLE_TO_PARSE_UNRECOGNIZED_FIELD, "requests[0].@reserved_field");
    }
}
