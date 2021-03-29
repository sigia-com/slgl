package io.slgl.api.it;

import io.slgl.api.it.data.TestDataUtils;
import io.slgl.api.it.properties.Props;
import io.slgl.api.it.user.User;
import io.slgl.api.utils.ErrorCode;
import io.slgl.client.SlglApiClient;
import io.slgl.client.Types;
import io.slgl.client.node.*;
import io.slgl.client.node.permission.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.utils.ErrorCode.NO_API_CREDITS_LEFT;
import static io.slgl.client.node.ReadState.NO_STATE;
import static io.slgl.client.node.permission.Allow.allowLink;
import static io.slgl.client.node.permission.Allow.allowReadState;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;

public class UserCreditsIT extends AbstractApiTest {

    private static final String ANCHOR = "#anchor";
    private NodeResponse nodeWithAnchor;

    @BeforeEach
    public void setUp() {
        nodeWithAnchor = createNodeWithAnchor();
    }

    @Test
    public void shouldNotAllowUserWithoutCredits() {
        var user = User.createUser();
        softly.assertApiException(() -> tryLink(user.getLedger()))
                .hasErrorCode(NO_API_CREDITS_LEFT);
        softly.assertApiException(() -> tryRead(user.getLedger()))
                .hasErrorCode(NO_API_CREDITS_LEFT);
    }

    @Test
    public void shouldAllowUserWithCredits() {
        var user = User.createUser().addCredits(Integer.MAX_VALUE);
        softly.assertThatCode(() -> tryLink(user.getLedger()))
                .doesNotThrowAnyException();
        softly.assertThatCode(() -> tryRead(user.getLedger()))
                .doesNotThrowAnyException();
    }

    @Test
    public void shouldAllowUserWithUsedWhenAddedCredits() {
        var user = User.createUser().addCredits(Integer.MAX_VALUE);
        waitForCredits(user);
        softly.assertThatCode(() -> tryLink(user.getLedger()))
                .doesNotThrowAnyException();
        softly.assertThatCode(() -> tryRead(user.getLedger()))
                .doesNotThrowAnyException();
    }

    @Test
    public void shouldCreateUserAndAddCreditsInSingleBatchRequet() {
        // given
        String apiKey = "example-api-key";

        WriteRequest request = WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(Types.USER))
                .addRequest(NodeRequest.builder()
                        .type(Types.KEY)
                        .data("value", apiKey))
                .addRequest(NodeRequest.builder()
                        .type(Types.CREDITS)
                        .data("credits_amount", 1000000000))
                .addLinkRequest(1, 0, "#keys")
                .addLinkRequest(2, 0, "#credits")
                .build();

        // when
        WriteResponse response = admin.write(request);

        // then
        String username = response.getNodes().get(0).getId();

        SlglApiClient newUserApiClient = SlglApiClient.builder()
                .apiUrl(Props.getSlglProperties().getLedgerUrl())
                .username(username)
                .apiKey(apiKey)
                .build();

        NodeResponse nodeCreatedByNodeUser = newUserApiClient.writeNode(NodeRequest.builder());
        assertThat(nodeCreatedByNodeUser).isNotNull();
    }

    @Test
    public void shouldValidateCreditsObject() {
        // when
        var error = expectError(() -> user.writeNode(NodeRequest.builder()
                .type(Types.CREDITS)));

        // then
        assertThat(error).hasErrorCode(ErrorCode.VALIDATION_ERROR)
                .hasFieldError("requests[0].credits_amount", "not_null");

        // when
        error = expectError(() -> user.writeNode(NodeRequest.builder()
                .type(Types.CREDITS)
                .data("credits_amount", 0)));

        // then
        assertThat(error).hasErrorCode(ErrorCode.VALIDATION_ERROR)
                .hasFieldError("requests[0].credits_amount", "min");
    }

    private void waitForCredits(User user) {
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThatCode(() -> user.getLedger().readNode(nodeWithAnchor.getId(), NO_STATE))
                        .doesNotThrowAnyException());
    }

    private NodeResponse createNodeWithAnchor() {
        var request = NodeRequest.builder()
                .id(TestDataUtils.generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor(ANCHOR)
                        .permission(Permission.builder()
                                .allow(allowReadState(), allowLink(ANCHOR))
                                .alwaysAllowed()))
                .build();
        return admin.getLedger().writeNode(request);
    }

    private void tryRead(SlglApiClient client) {
        client.readNode(nodeWithAnchor.getId(), NO_STATE);
    }

    private void tryLink(SlglApiClient client) {
        client.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, nodeWithAnchor, ANCHOR));
    }
}
