package io.slgl.permission.expression;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.expressions.PreEvaluatedArgumentsExpression;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;

import java.util.Arrays;
import java.util.List;

public class ContainsAnyOfExpression implements PreEvaluatedArgumentsExpression {

    private final ContainsExpression containsExpression = ContainsExpression.CONTAINS;

    @Override
    public String key() {
        return "contains_any_of";
    }

    @Override
    public Object evaluate(List arguments, Object data) throws JsonLogicEvaluationException {
        if (arguments.size() != 2) {
            throw new JsonLogicEvaluationException(key() + " expressions expect exactly 2 arguments");
        }

        Object left = arguments.get(0);
        Object right = arguments.get(1);

        if (!ArrayLike.isEligible(right)) {
            return containsExpression.evaluate(Arrays.asList(left, right), data);
        }

        for (Object rightItem : new ArrayLike(right)) {
            if (JsonLogic.truthy(containsExpression.evaluate(Arrays.asList(left, rightItem), data))) {
                return true;
            }
        }

        return false;
    }
}
