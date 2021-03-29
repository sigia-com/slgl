package io.slgl.client.audit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PermissionEvaluation {

    private final String node;
    private final String anchor;

    private final PermissionEvaluationType evaluationType;

    private final List<PermissionEvaluationResult> evaluationResults;

    private final boolean success;

    @JsonCreator
    public PermissionEvaluation(
            @JsonProperty("node") String node,
            @JsonProperty("anchor") String anchor,
            @JsonProperty("evaluation_type") PermissionEvaluationType evaluationType,
            @JsonProperty("evaluation_results") List<PermissionEvaluationResult> evaluationResults,
            @JsonProperty("success") boolean success
    ) {
        this.node = node;
        this.anchor = anchor;
        this.evaluationType = evaluationType;
        this.evaluationResults = evaluationResults;
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

    @JsonProperty("evaluation_type")
    public PermissionEvaluationType getEvaluationType() {
        return evaluationType;
    }

    @JsonProperty("evaluation_results")
    public List<PermissionEvaluationResult> getEvaluationResults() {
        return evaluationResults;
    }

    @JsonProperty("success")
    public boolean isSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return "PermissionEvaluation{" +
                "node='" + node + '\'' +
                ", anchor='" + anchor + '\'' +
                ", evaluationType=" + evaluationType +
                ", evaluationResults=" + evaluationResults +
                ", success=" + success +
                '}';
    }
}
