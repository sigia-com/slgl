package io.slgl.setup.actions;

import io.slgl.api.ExecutionContext;
import io.slgl.api.domain.ApiUser;
import io.slgl.api.protocol.ApiRequest;
import io.slgl.api.protocol.LinkRequest;
import io.slgl.api.protocol.NodeRequest;
import io.slgl.api.repository.NodeRepository;
import io.slgl.api.service.CurrentUserService;
import io.slgl.api.service.LedgerService;
import io.slgl.api.service.UserStateRepository;
import io.slgl.api.type.BuiltinType;
import io.slgl.api.utils.LambdaEnv;
import io.slgl.setup.CustomResourceRequestEvent;
import io.slgl.setup.CustomResourceResponseEvent.DataResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
public class AdminUserSetup {

    private final NodeRepository nodeRepository = ExecutionContext.get(NodeRepository.class);
    private final CurrentUserService currentUserService = ExecutionContext.get(CurrentUserService.class);
    private final LedgerService ledgerService = ExecutionContext.get(LedgerService.class);
    private final UserStateRepository userStateRepository = ExecutionContext.get(UserStateRepository.class);

    private static final String ADMIN = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

    public void execute(CustomResourceRequestEvent request, DataResponse response) {
        currentUserService.setCurrentUser(new ApiUser(ADMIN));

        try {
            if (!adminUserExists()) {
                createAdminUser();
                createAdminApiKey(request);
                addAdminCredits();
            }

            response.setDefaultAdminApiKey(getDefaultApiKey().orElse("<empty>"));

        } finally {
            currentUserService.setCurrentUser(null);
        }
    }

    private boolean adminUserExists() {
        return nodeRepository.readById(ADMIN) != null;
    }

    private void createAdminUser() {
        var userRequest = new ApiRequest()
                .addNode(new NodeRequest()
                        .setId(ADMIN)
                        .setType(BuiltinType.USER.getId())
                        .regenerateRawJson());

        log.info("Creating user: id={}", ADMIN);
        ledgerService.write(userRequest);
    }

    private void createAdminApiKey(CustomResourceRequestEvent request) {
        String apiKey = request.getResourceProperties().getAdminApiKey();
        boolean useDefaultApiKey = isBlank(apiKey);
        if (useDefaultApiKey) {
            apiKey = generateDefaultApiKey();
        }

        var nodeRequest = new NodeRequest()
                .setType(BuiltinType.KEY.getId())
                .setData("value", apiKey)
                .regenerateRawJson();
        var linkRequest = new LinkRequest()
                .setSourceNode(0)
                .setTargetNode(ADMIN)
                .setTargetAnchor("#keys");

        log.info("Creating user's api key: id={}, link={}", nodeRequest.getId(), linkRequest.getTargetNode());

        ledgerService.write(new ApiRequest()
                .addNode(nodeRequest)
                .addLink(linkRequest));
    }

    private void addAdminCredits() {
        var request = new ApiRequest()
                .addNode(new NodeRequest()
                        .setType(BuiltinType.CREDITS.getId())
                        .setData("credits_amount", LambdaEnv.getInitialUserCredits())
                        .regenerateRawJson())
                .addLink(new LinkRequest()
                        .setSourceNode(0)
                        .setTargetNode(ADMIN)
                        .setTargetAnchor("#credits"));

        ledgerService.write(request);
    }

    private Optional<String> getDefaultApiKey() {
        return userStateRepository.getFirstKey(ADMIN);
    }

    private static String generateDefaultApiKey() {
        return "sk_live_" + RandomStringUtils.randomAlphanumeric(24);
    }
}
