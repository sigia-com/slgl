package io.slgl.api.permission;

import io.slgl.client.audit.PermissionEvaluation;
import io.slgl.client.audit.PermissionEvaluationResult;
import io.slgl.client.audit.PermissionEvaluationType;

import java.util.ArrayList;
import java.util.List;

public class PermissionEvaluationBuilder {

    private PermissionEvaluationType evaluationType;
    private String node;
    private String anchor;
    private List<PermissionEvaluationResult> evaluationResults = new ArrayList<>();
    private boolean success;

    public PermissionEvaluationBuilder evaluationType(PermissionEvaluationType evaluationType) {
        this.evaluationType = evaluationType;
        return this;
    }

    public PermissionEvaluationBuilder node(String node) {
        this.node = node;
        return this;
    }

    public PermissionEvaluationBuilder anchor(String anchor) {
        this.anchor = anchor;
        return this;
    }

    public PermissionEvaluationBuilder addEvaluationResults(PermissionEvaluationResult evaluationResult) {
        evaluationResults.add(evaluationResult);
        return this;
    }

    public PermissionEvaluationBuilder evaluationResults(List<PermissionEvaluationResult> evaluationResults) {
        this.evaluationResults = evaluationResults;
        return this;
    }

    public PermissionEvaluationBuilder success(boolean success) {
        this.success = success;
        return this;
    }

    public boolean isSuccess() {
        return success;
    }

    public PermissionEvaluation build() {
        return new PermissionEvaluation(node, anchor, evaluationType, evaluationResults, success);
    }
}
