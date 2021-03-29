package io.slgl.api.service;

import io.slgl.api.domain.ApiUser;

public class CurrentUserService {

    private ThreadLocal<ApiUser> currentUser = new ThreadLocal<>();
    private ThreadLocal<ApiUser> permissionsUser = new ThreadLocal<>();

    public ApiUser getCurrentUser() {
        return currentUser.get();
    }

    public void setCurrentUser(ApiUser user) {
        currentUser.set(user);
    }

    public void setPermissionsUser(ApiUser user) {
        permissionsUser.set(user);
    }

    public ApiUser getPermissionsUser() {
        return permissionsUser.get();
    }
}
