package io.slgl.api.permission;

import io.slgl.client.audit.EvaluationLogEntry;
import io.slgl.client.audit.PermissionAudit;
import io.slgl.client.audit.PermissionEvaluation;
import io.slgl.client.audit.RequestType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PermissionAuditBuilder {

    private String node;
    private String anchor;

    private RequestType requestType;

    private Instant evaluationTime;

    private List<PermissionEvaluation> evaluatedPermissions = new ArrayList<>();
    private List<EvaluationLogEntry> evaluationLog = new ArrayList<>();

    private Boolean forceSuccess;

    public PermissionAudit build() {
        return new PermissionAudit(node, anchor, requestType, evaluationTime, evaluatedPermissions, evaluationLog, isSuccess());
    }

    private boolean isSuccess() {
        if (forceSuccess != null) {
            return forceSuccess;
        }

        return evaluatedPermissions.stream()
                .allMatch(PermissionEvaluation::isSuccess);
    }

    public PermissionAuditBuilder node(String node) {
        this.node = node;
        return this;
    }

    public PermissionAuditBuilder anchor(String anchor) {
        this.anchor = anchor;
        return this;
    }

    public PermissionAuditBuilder requestType(RequestType requestType) {
        this.requestType = requestType;
        return this;
    }

    public PermissionAuditBuilder evaluationTime(Instant evaluationTime) {
        this.evaluationTime = evaluationTime;
        return this;
    }

    public PermissionAuditBuilder addEvaluatedPermission(PermissionEvaluation evaluatedPermission) {
        evaluatedPermissions.add(evaluatedPermission);
        return this;
    }

    public PermissionAuditBuilder evaluatedPermissions(List<PermissionEvaluation> evaluatedPermissions) {
        this.evaluatedPermissions = evaluatedPermissions;
        return this;
    }

    public PermissionAuditBuilder addEvaluationLog(EvaluationLogEntry evaluationLog) {
        this.evaluationLog.add(evaluationLog);
        return this;
    }

    public PermissionAuditBuilder addEvaluationLog(List<EvaluationLogEntry> evaluationLog) {
        this.evaluationLog = evaluationLog;
        return this;
    }

    public PermissionAuditBuilder forceSuccess(boolean forceSuccess) {
        this.forceSuccess = forceSuccess;
        return this;
    }

    public boolean isEmpty() {
        return evaluatedPermissions.isEmpty() && evaluationLog.isEmpty();
    }
}
