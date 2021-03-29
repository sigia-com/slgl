package io.slgl.api.domain;

import io.slgl.api.context.principal.ApiPrincipal;

public class ApiUser {

    private final String userId;

    public ApiUser(String userId) {
        this.userId = userId;
    }

    public ApiPrincipal asPrincipal() {
        return new ApiPrincipal()
                .setUsername(userId);
    }

    public String getId() {
        return this.userId;
    }

}
