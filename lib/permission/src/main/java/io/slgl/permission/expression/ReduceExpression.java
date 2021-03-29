package io.slgl.permission.expression;

import io.github.jamsesso.jsonlogic.ast.JsonLogicArray;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpression;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;
import io.slgl.permission.context.EvaluationContext;

import java.util.HashMap;

public class ReduceExpression implements JsonLogicExpression {

    @Override
    public String key() {
        return "reduce";
    }

    @Override
    public Object evaluate(JsonLogicEvaluator evaluator, JsonLogicArray arguments, Object data)
            throws JsonLogicEvaluationException {
        if (arguments.size() != 3) {
            throw new JsonLogicEvaluationException("reduce expects exactly 3 arguments");
        }

        Object maybeArray = evaluator.evaluate(arguments.get(0), data);
        Object accumulator = evaluator.evaluate(arguments.get(2), data);

        if (!ArrayLike.isEligible(maybeArray)) {
            return accumulator;
        }

        EvaluationContext context = EvaluationContext.wrap(data);

        for (Object item : new ArrayLike(maybeArray)) {
            EvaluationContext localContext = createExtendedContext(accumulator, context, item);
            accumulator = evaluator.evaluate(arguments.get(1), localContext);
        }

        return accumulator;
    }

    public EvaluationContext createExtendedContext(Object accumulator, EvaluationContext context, Object item) {
        HashMap<Object, Object> values = new HashMap<>();
        values.put("$current", item);
        values.put("$accumulator", accumulator);
        return EvaluationContext.of(values).withParentContext(context);
    }

}
