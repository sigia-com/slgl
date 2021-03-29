package io.slgl.api.service;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import io.slgl.api.ExecutionContext;
import io.slgl.api.domain.ApiUser;

import java.util.Optional;

public class CurrentUserSetter implements ExecutionContext.OnRequestCallback, ExecutionContext.PostExecutionCallback {

    private final CurrentUserService currentUserService = ExecutionContext.get(CurrentUserService.class);

    @Override
    public void onRequest(APIGatewayProxyRequestEvent request) {
        Optional.ofNullable(request)
                .map(APIGatewayProxyRequestEvent::getRequestContext)
                .map(APIGatewayProxyRequestEvent.ProxyRequestContext::getAuthorizer)
                .map(el -> el.get("user_id"))
                .map(Object::toString)
                .ifPresentOrElse(this::setUserContext, this::reset);

    }

    private void setUserContext(String userId) {
        var user = new ApiUser(userId);
        currentUserService.setCurrentUser(user);
    }

    private void reset() {
        currentUserService.setCurrentUser(null);
    }

    @Override
    public void afterExecution() {
        reset();
    }
}
