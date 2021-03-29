package io.slgl.permission.expression;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.expressions.PreEvaluatedArgumentsExpression;
import io.slgl.permission.JsonLogicVariableEvaluator;
import io.slgl.permission.PermissionEvaluationLogger;

import java.util.List;

public class StaticNodeExpression implements PreEvaluatedArgumentsExpression {

    private final JsonLogicVariableEvaluator variableEvaluator;

    public StaticNodeExpression(PermissionEvaluationLogger logger) {
        this.variableEvaluator = new JsonLogicVariableEvaluator(false, logger);
    }

    @Override
    public String key() {
        return "node";
    }

    @Override
    public Object evaluate(List arguments, Object data) throws JsonLogicEvaluationException {
        if (arguments.size() < 1) {
            return null;
        }

        String id = String.valueOf(arguments.get(0));
        return variableEvaluator.evaluateVariable("@(" + id + ")", data, null);
    }
}
