package io.slgl.client.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.slgl.client.Types;

public final class AuditorNodeRequest extends NodeRequest {

    private AuditorNodeRequest(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    public enum AuditPolicy {
        @JsonProperty("all")
        ALL,
        @JsonProperty("succeed")
        SUCCEED,
        @JsonProperty("failed")
        FAILED
    }

    public static class Builder extends NodeRequest.Builder<Builder> {

        {
            super.type(Types.AUDITOR);
        }

        public Builder authorizedUser(String username) {
            this.data("authorized_user", username);
            return this;
        }

        public Builder awsSqs(String sqs) {
            this.data("aws_sqs", sqs);
            return this;
        }

        public Builder auditPolicy(AuditPolicy policy) {
            this.data("audit_policy", policy);
            return this;
        }

        @Override
        public AuditorNodeRequest build() {
            return new AuditorNodeRequest(this);
        }
    }


}
