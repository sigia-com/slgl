package io.slgl.permission.expression;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.expressions.PreEvaluatedArgumentsExpression;
import io.slgl.permission.JsonLogicVariableEvaluator;
import io.slgl.permission.PermissionEvaluationLogger;

import java.util.List;

public class VarMapExpression implements PreEvaluatedArgumentsExpression {

    private final JsonLogicVariableEvaluator variableEvaluator;

    public VarMapExpression(PermissionEvaluationLogger logger) {
        this.variableEvaluator = new JsonLogicVariableEvaluator(true, logger);
    }

    @Override
    public String key() {
        return "var_map";
    }

    @Override
    public Object evaluate(List arguments, Object data) throws JsonLogicEvaluationException {
        if (arguments.size() < 1) {
            return null;
        }

        String path = String.valueOf(arguments.get(0));

        if (arguments.size() >= 2) {
            data = arguments.get(1);
        }

        return variableEvaluator.evaluateVariable(path, data, null);
    }
}
