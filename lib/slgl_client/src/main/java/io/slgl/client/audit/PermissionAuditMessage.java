package io.slgl.client.audit;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PermissionAuditMessage {
    private final String authorizedUser;
    private final PermissionAudit permissionAudit;

    @JsonCreator
    public PermissionAuditMessage(
            @JsonProperty("authorized_user") String authorizedUser,
            @JsonProperty("permission_audit") PermissionAudit permissionAudit
    ) {
        this.authorizedUser = authorizedUser;
        this.permissionAudit = permissionAudit;
    }

    @JsonProperty("authorized_user")
    public String getAuthorizedUser() {
        return authorizedUser;
    }

    @JsonProperty("permission_audit")
    public PermissionAudit getPermissionAudit() {
        return permissionAudit;
    }

    @Override
    public String toString() {
        return "AuditData{" +
                "authorizedUser='" + authorizedUser + '\'' +
                ", evaluatedPermissions=" + permissionAudit +
                '}';
    }
}
