package io.slgl.api.it;

import io.slgl.api.it.user.User;
import io.slgl.api.utils.ErrorCode;
import io.slgl.client.node.NodeRequest;
import org.junit.jupiter.api.Test;

import static io.slgl.api.it.assertj.SlglAssertions.assertThat;

public class KeyIT extends AbstractApiTest {

    @Test
    public void shouldAllowToCreateNodeUsingCreatedKey() {
        // given
        var user = User.createUser().addCredits(1);

        var request = NodeRequest.builder()
                .data("example_key", "example_value")
                .build();

        // when
        var response = user.writeNode(request);

        // then
        assertThat(response).isNotNull();
    }

    @Test
    public void shouldDenyCreatingNodeIfKeyWasDeleted() {
        // given
        var user = User.createUser();
        user.deleteKey(user.getSecretKeys().get(0));

        var request = NodeRequest.builder()
                .data("example_key", "example_value")
                .build();

        // when
        var error = expectError(() -> user.writeNode(request));

        // then
        assertThat(error).hasErrorCode(ErrorCode.INVALID_API_KEY);
    }
}
