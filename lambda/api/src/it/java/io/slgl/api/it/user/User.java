package io.slgl.api.it.user;

import io.slgl.api.it.properties.Props;
import io.slgl.client.SlglApiClient;
import io.slgl.client.Types;
import io.slgl.client.node.*;
import lombok.Getter;
import lombok.experimental.Delegate;

import java.util.ArrayList;
import java.util.List;

import static io.slgl.api.it.data.UserMother.*;
import static io.slgl.api.it.utils.CleanerHolder.registerInCleaner;

public class User {

    public static final User ADMIN;

    static {
        ADMIN = new User(Props.getSlglProperties().getAdminUsername(), Props.getSlglProperties().getAdminApiKey());

        try {
            ADMIN.addCredits(1_000_000_000_000_000L);
        } catch (Exception ignore) {
        }
    }

    @Getter
    private final String username;
    @Getter
    private final List<SecretKey> secretKeys = new ArrayList<>();

    @Getter
    @Delegate
    protected SlglApiClient ledger;

    public User(String username, String secretKey) {
        this.username = username;
        if (secretKey != null) {
            this.secretKeys.add(new SecretKey(secretKey, null, null));
        }
        setLedgerClient(username, secretKey);
    }

    private void setLedgerClient(String username, String apiKey) {
        ledger = registerInCleaner(
                SlglApiClient.builder()
                        .apiUrl(Props.getSlglProperties().getLedgerUrl())
                        .username(username)
                        .apiKey(apiKey)
                        .requestListener(StateStorage.INSTANCE)
                        .responseListener(StateStorage.INSTANCE)
                        .build()
        );
    }

    public static User createUser() {
        var request = createUserRequest();
        var entryResponse = ADMIN.writeNode(request);
        var username = entryResponse.getId();
        var user = new User(username, null);
        var key = user.addKey();
        user.setLedgerClient(username, key.getSecretKey());
        return user;
    }

    public SecretKey addKey() {
        var newApiKey = generateUniqueKey();
        var newKeyRequest = createNewKeyRequest(getUsername(), newApiKey);
        var response = ADMIN.write(newKeyRequest);
        var secretKey = new SecretKey(newApiKey, response.getNodes().get(0), response.getLinks().get(0));
        secretKeys.add(secretKey);
        return secretKey;
    }

    public WriteResponse deleteKey(SecretKey secretKey) {
        var request = createDeleteKeyRequest(secretKey.getLinkResponse().getId());
        return ADMIN.write(request);
    }

    public String getSecretKey() {
        return secretKeys.get(0).getSecretKey();
    }

    public AuditorNodeRequest.Builder auditor(AuditorNodeRequest.AuditPolicy auditPolicy) {
        return AuditorNodeRequest.builder()
                .auditPolicy(auditPolicy)
                .authorizedUser(getUsername())
                .awsSqs(Props.getSlglProperties().getAuditorSqsQueueUrl());
    }

    public User addCredits(long maxValue) {
        ADMIN.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(Types.CREDITS)
                        .data("credits_amount", maxValue))
                .addRequest(LinkRequest.builder()
                        .sourceNode(0)
                        .targetNode(getUsername())
                        .targetAnchor("#credits")));

        return this;
    }
}
