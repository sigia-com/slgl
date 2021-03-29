package io.slgl.permission.expression;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.expressions.EqualityExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.PreEvaluatedArgumentsExpression;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;

import java.util.Arrays;
import java.util.List;

public class ContainsExpression implements PreEvaluatedArgumentsExpression {

    public static final ContainsExpression CONTAINS = new ContainsExpression(true);
    public static final ContainsExpression DOES_NOT_CONTAIN = new ContainsExpression(false);

    private final EqualityExpression equalityExpression = EqualityExpression.INSTANCE;

    private final boolean contains;

    private ContainsExpression(boolean contains) {
        this.contains = contains;
    }

    @Override
    public String key() {
        return contains ? "contains" : "does_not_contain";
    }

    @Override
    public Object evaluate(List arguments, Object data) throws JsonLogicEvaluationException {
        if (arguments.size() != 2) {
            throw new JsonLogicEvaluationException(key() + " expressions expect exactly 2 arguments");
        }

        Object left = arguments.get(0);
        Object right = arguments.get(1);

        if (!ArrayLike.isEligible(left)) {
            return null;
        }

        for (Object leftItem : new ArrayLike(left)) {
            if (JsonLogic.truthy(equalityExpression.evaluate(Arrays.asList(leftItem, right), data))) {
                return contains;
            }
        }

        return !contains;
    }
}
