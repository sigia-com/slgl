package io.slgl.client.audit;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.slgl.client.node.permission.Permission;

import java.util.List;
import java.util.Map;

public class PermissionEvaluationResult {

    private final Permission permission;
    private final Map<String, Object> evaluationContext;
    private final List<EvaluationLogEntry> evaluationLog;
    private final boolean success;

    @JsonCreator
    public PermissionEvaluationResult(
            @JsonProperty("permission") Permission permission,
            @JsonProperty("context") Map<String, Object> evaluationContext,
            @JsonProperty("evaluation_log") List<EvaluationLogEntry> evaluationLog,
            @JsonProperty("success") boolean success
    ) {
        this.permission = permission;
        this.evaluationContext = evaluationContext;
        this.evaluationLog = evaluationLog;
        this.success = success;
    }

    @JsonProperty("permission")
    public Permission getPermission() {
        return permission;
    }

    @JsonInclude
    @JsonProperty("evaluation_context")
    public Map<String, Object> getEvaluationContext() {
        return evaluationContext;
    }

    @JsonProperty("evaluation_log")
    public List<EvaluationLogEntry> getEvaluationLog() {
        return evaluationLog;
    }

    @JsonProperty("success")
    public boolean isSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return "PermissionEvaluationResult{" +
                "permission=" + permission +
                ", evaluationContext=" + evaluationContext +
                ", evaluationLog=" + evaluationLog +
                ", success=" + success +
                '}';
    }
}
