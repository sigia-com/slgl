package io.slgl.client.node.authorization;

import io.slgl.client.Types;
import io.slgl.client.jsonlogic.CustomJsonLogic;
import io.slgl.client.jsonlogic.JsonLogic;
import io.slgl.client.node.NodeRequest;
import io.slgl.client.node.permission.Requirement;
import io.slgl.client.node.permission.Requirements;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public final class AuthorizationNodeRequest extends NodeRequest {

    private AuthorizationNodeRequest(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends NodeRequest.Builder<Builder> {
        
        private List<Authorize> authorize;

        private Requirements.Builder requireBuilder = Requirements.builder();
        private JsonLogic requireLogic;

        {
            super.type(Types.AUTHORIZATION);
        }

        public Builder authorize(Authorize... allows) {
            return authorize(asList(allows));
        }

        public Builder authorize(List<Authorize> allows) {
            if (authorize == null) {
                authorize = new ArrayList<>();
            }

            authorize.addAll(allows);
            return this;
        }

        public Builder alwaysAllowed() {
            requireBuilder = Requirements.builder();
            requireLogic(null);
            return this;
        }

        public Builder requireAll(Requirement... requirements) {
            require().require(requirements);
            return this;
        }

        public Builder requireAll(Iterable<Requirement> requirements) {
            require().require(requirements);
            return this;
        }

        public Builder require(Requirements requirements) {
            require().require(requirements);
            return this;
        }

        public Builder require(Requirements.Builder requirements) {
            require().require(requirements);
            return this;
        }

        private Requirements.Builder require() {
            return requireBuilder;
        }

        public Builder requireLogic(Object requireLogic) {
            this.requireLogic = new CustomJsonLogic(requireLogic);
            return this;
        }

        public Builder requireLogic(JsonLogic requireLogic) {
            this.requireLogic = requireLogic;
            return this;
        }

        @Override
        public AuthorizationNodeRequest build() {
            if (authorize != null) {
                data("authorize", authorize);
            }

            if (requireBuilder != null) {
                data("require", requireBuilder.build());
            }

            if (requireLogic != null) {
                data("require_logic", requireLogic);
            }

            return new AuthorizationNodeRequest(this);
        }
    }
}
