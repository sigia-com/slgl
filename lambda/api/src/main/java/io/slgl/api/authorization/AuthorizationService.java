package io.slgl.api.authorization;

import io.slgl.api.ExecutionContext;
import io.slgl.api.authorization.model.Authorize;
import io.slgl.api.context.EvaluationContextBuilder;

import java.util.List;

import static io.slgl.api.utils.CollectionUtils.nullToEmptyList;

public class AuthorizationService {

    private AuthorizationFactory authorizationFactory = ExecutionContext.get(AuthorizationFactory.class);

    public Authorization processAuthorization(
            Authorize authorizeForAction, List<String> authorizationIds, EvaluationContextBuilder contextBuilder) {

        Authorization currentAuthorization = null;

        for (String authorizationId : nullToEmptyList(authorizationIds)) {
            Authorization authorization = authorizationFactory.get(authorizationId);

            authorization.verifyIsMatchingAction(authorizeForAction);
            authorization.verifyPermissions(contextBuilder, currentAuthorization);

            currentAuthorization = authorization;
        }

        return currentAuthorization;
    }
}
