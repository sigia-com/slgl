package io.slgl.client.audit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public class PermissionAudit {

    private final String node;
    private final String anchor;

    private final RequestType requestType;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final Instant evaluationTime;

    private final List<PermissionEvaluation> evaluatedPermissions;
    private final List<EvaluationLogEntry> evaluationLog;

    private final boolean success;

    @JsonCreator
    public PermissionAudit(
            @JsonProperty("node") String node,
            @JsonProperty("anchor") String anchor,
            @JsonProperty("request_type") RequestType requestType,
            @JsonProperty("evaluation_time") Instant evaluationTime,
            @JsonProperty("evaluated_permissions") List<PermissionEvaluation> evaluatedPermissions,
            @JsonProperty("evaluation_log") List<EvaluationLogEntry> evaluationLog,
            @JsonProperty("success") boolean success
    ) {
        this.node = node;
        this.anchor = anchor;
        this.requestType = requestType;
        this.evaluationTime = evaluationTime;
        this.evaluationLog = evaluationLog;
        this.evaluatedPermissions = evaluatedPermissions;
        this.success = success;
    }

    @JsonProperty("node")
    public String getNode() {
        return node;
    }

    @JsonProperty("anchor")
    public String getAnchor() {
        return anchor;
    }

    @JsonProperty("request_type")
    public RequestType getRequestType() {
        return requestType;
    }

    @JsonProperty("evaluation_time")
    public Instant getEvaluationTime() {
        return evaluationTime;
    }

    @JsonProperty("evaluation_log")
    public List<EvaluationLogEntry> getEvaluationLog() {
        return evaluationLog;
    }

    @JsonProperty("evaluated_permissions")
    public List<PermissionEvaluation> getEvaluatedPermissions() {
        return evaluatedPermissions;
    }

    @JsonProperty("success")
    public boolean isSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return "PermissionAudit{" +
                "node='" + node + '\'' +
                ", anchor='" + anchor + '\'' +
                ", requestType=" + requestType +
                ", evaluationTime=" + evaluationTime +
                ", evaluatedPermissions=" + evaluatedPermissions +
                ", evaluationLog=" + evaluationLog +
                ", success=" + success +
                '}';
    }
}
