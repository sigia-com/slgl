package io.slgl.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.slgl.api.validator.ValidUrl;
import io.slgl.client.audit.PermissionAudit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.util.function.Predicate;

@Accessors(chain = true)
@Getter
@Setter
public class AuditorEntity {

    @JsonProperty("authorized_user")
    private String authorizedUser;

    @JsonProperty("aws_sqs")
    @NotNull
    @ValidUrl
    private String awsSqs;

    @JsonProperty("audit_policy")
    @NotNull
    private AuditPolicy auditPolicy;

    @AllArgsConstructor
    public enum AuditPolicy {

        @JsonProperty("all")
        ALL(summary -> true),
        @JsonProperty("succeed")
        SUCCEED(summary -> summary.isSuccess()),
        @JsonProperty("failed")
        FAILED(summary -> !summary.isSuccess());

        private final Predicate<PermissionAudit> shouldSend;

        public boolean shouldSendAudit(PermissionAudit summary) {
            return shouldSend.test(summary);
        }
    }
}
