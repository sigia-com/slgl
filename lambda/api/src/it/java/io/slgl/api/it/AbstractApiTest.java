package io.slgl.api.it;

import io.slgl.api.it.assertj.SlglSoftAssertions;
import io.slgl.api.it.junit.extension.TestUserExtension;
import io.slgl.api.it.properties.Props;
import io.slgl.api.it.user.User;
import io.slgl.client.SlglApiClient;
import io.slgl.client.error.ErrorResponse;
import io.slgl.client.error.SlglApiException;
import io.slgl.client.node.NodeRequest;
import io.slgl.client.node.NodeResponse;
import io.slgl.client.node.TypeNodeRequest;
import io.slgl.client.node.permission.Permission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;
import static io.slgl.api.it.utils.CleanerHolder.registerInCleaner;
import static io.slgl.client.node.permission.Allow.allowAll;
import static org.assertj.core.api.Assertions.fail;

@ExtendWith(TestUserExtension.class)
public class AbstractApiTest {

    public static TestUserExtension testUserExtension = new TestUserExtension();

    protected User user;
    protected User admin = User.ADMIN;
    protected SlglSoftAssertions softly;

    protected SlglApiClient ledger;

    @BeforeAll
    static void setupAll(TestUserExtension testUserExtension) {
        AbstractApiTest.testUserExtension = testUserExtension;
    }

    @BeforeEach
    public void setupAbstractApiTest() {
        user = testUserExtension.getSharedTestUser();
        ledger = user.getLedger();
        softly = new SlglSoftAssertions();
    }

    @AfterEach
    public void tearDownAbstractApiTest() {
        softly.assertAll();
        softly = null;
    }

    protected User getSecondUser() {
        return testUserExtension.getSharedSecondTestUser();
    }

    protected User getThirdUser() {
        return testUserExtension.getSharedThirdTestUser();
    }

    protected SlglApiClient getAnonymousApiClient() {
        return registerInCleaner(
                SlglApiClient.builder()
                        .apiUrl(Props.getSlglProperties().getLedgerUrl())
                        .build()
        );
    }

    protected ErrorResponse expectError(Runnable request) {
        try {
            request.run();

            return fail("Error expected");

        } catch (SlglApiException e) {
            return e.getErrorResponse();
        }
    }


    protected NodeResponse createType(String name) {
        TypeNodeRequest.Builder request = TypeNodeRequest.builder()
                .id(generateUniqueId());

        return ledger.writeNode(request.build());
    }

    protected NodeResponse writeNode(String name) {
        NodeRequest.Builder<?> request = NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .permission(Permission.builder().allow(allowAll()).alwaysAllowed()));

        return ledger.writeNode(request.build());
    }

    protected NodeResponse writeNode(String name, NodeResponse type) {
        NodeRequest.Builder<?> request = NodeRequest.builder()
                .id(generateUniqueId())
                .type(type.getId());

        return ledger.writeNode(request.build());
    }

    protected NodeResponse writeNode(String name, TypeNodeRequest.Builder type) {
        NodeRequest.Builder<?> request = NodeRequest.builder()
                .id(generateUniqueId())
                .type(type);

        return ledger.writeNode(request.build());
    }
}
