package io.slgl.api.it;

import com.sun.jna.Platform;
import io.slgl.api.it.user.User;
import io.slgl.api.utils.ErrorCode;
import io.slgl.client.error.ErrorResponse;
import io.slgl.client.node.NodeRequest;
import io.slgl.client.node.NodeResponse;
import io.slgl.client.node.ReadState;
import io.slgl.client.node.TypeNodeRequest;
import org.junit.jupiter.api.Test;

import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.it.data.PermissionMother.readStateRequiringUsername;
import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;
import static io.slgl.api.utils.Utils.getSha3OnJqSCompliantJson;
import static io.slgl.api.utils.json.UncheckedObjectMapper.MAPPER;

public class ReadNodeIT extends AbstractApiTest {

    @Test
    public void shouldReadEntryById() {
        // given
        NodeResponse entry = writeNode();

        // when
        NodeResponse response = ledger.readNode(entry.getId(), ReadState.NO_STATE);

        // then
        assertThat(response)
                .usingRecursiveComparison()
                .ignoringFields("state")
                .isEqualTo(entry);
    }

    @Test
    public void shouldReturnStateWhenAskingForItAsCreatingUser() {
        // given
        NodeResponse entry = writeNodeWithState();

        // when
        NodeResponse response = ledger.readNode(entry.getId(), ReadState.WITH_STATE);

        // then
        assertThat(response.getObjectSha3()).isNotBlank();
        assertThat(response.getStateSha3()).isNotBlank();
        assertThat(response.getFileSha3()).isNull();
        assertThat(response).usingRecursiveComparison().isNotEqualTo(entry);

        if (!Platform.isWindows()) {
            var expectedStateSha = getSha3OnJqSCompliantJson(MAPPER.writeValueAsString(response.getState()));
            assertThat(response.getStateSha3()).isEqualTo(expectedStateSha);
        }
    }

    @Test
    public void shouldReturnStateWhenReadStatePermissionGivenToDifferentUser() {
        // given
        User otherUser = getSecondUser();

        NodeResponse entry = writeNodeWithStateAndReadStatePermissionGivenTo(otherUser.getUsername());

        // when
        NodeResponse response = otherUser.readNode(entry.getId(), ReadState.WITH_STATE);

        // then
        assertThat(response.getObjectSha3()).isNotBlank();
        assertThat(response.getStateSha3()).isNotBlank();
        assertThat(response.getFileSha3()).isNull();
        assertThat(response).usingRecursiveComparison().isNotEqualTo(entry);

        if (!Platform.isWindows()) {
            var expectedStateSha = getSha3OnJqSCompliantJson(MAPPER.writeValueAsString(response.getState()));
            assertThat(response.getStateSha3()).isEqualTo(expectedStateSha);
        }
    }

    @Test
    public void shouldFailWhenAskingForStateByNotPermittedUser() {
        // given
        NodeResponse entry = writeNodeWithState();

        // when
        ErrorResponse error = expectError(() -> getSecondUser().readNode(entry.getId(), ReadState.WITH_STATE));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldFailWhenAskingForStateWithoutUser() {
        // given
        NodeResponse entry = writeNodeWithState();

        // when
        ErrorResponse error = expectError(() -> getAnonymousApiClient().readNode(entry.getId(), ReadState.WITH_STATE));

        // then
        assertThat(error).hasErrorCode(ErrorCode.INVALID_API_KEY);
    }

    private NodeResponse writeNode() {
        NodeRequest.Builder<?> request = NodeRequest.builder()
                .id(generateUniqueId())
                .data("example_key", "example_value");

        return ledger.writeNode(request);
    }

    private NodeResponse writeNodeWithState() {
        return writeNodeWithStateAndReadStatePermissionGivenTo(user.getUsername());
    }

    private NodeResponse writeNodeWithStateAndReadStatePermissionGivenTo(String username) {
        return ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .stateProperties("example")
                        .permission(readStateRequiringUsername(username)))
                .data("example", "value"));
    }
}
