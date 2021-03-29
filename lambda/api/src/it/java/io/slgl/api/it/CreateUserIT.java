package io.slgl.api.it;

import io.slgl.api.it.user.StateStorage;
import io.slgl.api.it.utils.HashUtils;
import io.slgl.api.utils.ErrorCode;
import io.slgl.client.Types;
import io.slgl.client.error.ErrorResponse;
import io.slgl.client.node.NodeRequest;
import io.slgl.client.node.ReadState;
import org.junit.jupiter.api.Test;

import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.it.data.UserMother.createNewKeyRequest;
import static io.slgl.api.it.data.UserMother.createUserRequest;

public class CreateUserIT extends AbstractApiTest {

    @Test
    public void shouldCreateUserWhenCreatedByAdmin() {
        // given
        var request = createUserRequest().build();

        // when
        var response = admin.writeNode(request);

        // then
        var got = admin.readNode(response.getId(), ReadState.NO_STATE);
        assertThat(got.getObjectSha3()).isNotNull();
        assertThat(got.getFileSha3()).isNull();
        assertThat(got.getStateSha3()).isNull();
        assertThat(got.getId()).isEqualTo(request.getId());
    }

    @Test
    public void shouldAddKeyByAdmin() {
        // given
        var userRequest = createUserRequest().build();
        admin.writeNode(userRequest);
        var keyRequest = createNewKeyRequest(userRequest.getId());
        StateStorage.ignoreStateStorageForRequest(keyRequest);

        // when
        var keyResponse = admin.write(keyRequest);

        // then
        var expectedObjectSha3 = HashUtils.sha3_512("{\n" +
                "  \"@type\": \"" + Types.KEY + "\",\n" +
                "  \"value\": \"" + ((NodeRequest) keyRequest.getRequests().get(0)).getData().get("value") + "\"\n" +
                "}\n");

        var got = admin.readNode(keyResponse.getNodes().get(0).getId(), ReadState.NO_STATE);
        assertThat(got.getObjectSha3()).isEqualTo(expectedObjectSha3);
        assertThat(got.getFileSha3()).isNull();
        assertThat(got.getId()).isNotEmpty();
    }

    @Test
    public void shouldFailWhenAddingKeyNotByAdmin() {
        // given
        var userRequest = createUserRequest().build();
        admin.writeNode(userRequest);

        var keyRequest = createNewKeyRequest(userRequest.getId());

        // when
        ErrorResponse error = expectError(() -> user.write(keyRequest));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }
}
