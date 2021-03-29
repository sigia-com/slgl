package io.slgl.permission.expression;

import io.github.jamsesso.jsonlogic.ast.JsonLogicArray;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.ast.JsonLogicString;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpression;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;
import io.slgl.permission.context.EvaluationContext;

import java.util.Collections;
import java.util.List;

public abstract class AbstractArrayExpression implements JsonLogicExpression {

    @Override
    public Object evaluate(JsonLogicEvaluator evaluator, JsonLogicArray arguments, Object data)
            throws JsonLogicEvaluationException {

        if (arguments.size() != 2 && arguments.size() != 3) {
            throw new JsonLogicEvaluationException("expression '" + key() + "' expects exactly 2 or 3 arguments");
        }

        Object maybeArray = evaluator.evaluate(arguments.get(0), data);
        if (maybeArray == null) {
            maybeArray = Collections.emptyList();
        }

        if (!ArrayLike.isEligible(maybeArray)) {
            throw new JsonLogicEvaluationException("expression '" + key() + "' expects 1st argument to be a valid array");
        }

        if (arguments.size() == 3 && !(arguments.get(1) instanceof JsonLogicString)) {
            throw new JsonLogicEvaluationException("expression '" + key() + "' expects 2nd argument to be a string");
        }

        List<?> array = maybeArray instanceof List ? ((List<?>) maybeArray) : new ArrayLike(maybeArray);
        String itemKey = arguments.size() == 3 ? ((JsonLogicString) arguments.get(1)).getValue() : "$current";
        JsonLogicNode expression = arguments.size() == 3 ? arguments.get(2) : arguments.get(1);

        EvaluationContext context = EvaluationContext.wrap(data);

        return evaluateArray(evaluator, array, itemKey, expression, context);
    }

    protected abstract Object evaluateArray(JsonLogicEvaluator evaluator, List<?> array, String itemKey,
                                            JsonLogicNode expression, EvaluationContext context)
            throws JsonLogicEvaluationException;
}
