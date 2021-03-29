package io.slgl.permission.expression;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.slgl.permission.context.EvaluationContext;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonMap;

public class FilterExpression extends AbstractArrayExpression {

    @Override
    public String key() {
        return "filter";
    }

    @Override
    protected Object evaluateArray(JsonLogicEvaluator evaluator, List<?> array, String itemKey,
                                   JsonLogicNode expression, EvaluationContext context)
            throws JsonLogicEvaluationException {

        List<Object> result = new ArrayList<>();

        for (Object item : array) {
            EvaluationContext localContext = EvaluationContext.of(singletonMap(itemKey, item)).withParentContext(context);
            if (JsonLogic.truthy(evaluator.evaluate(expression, localContext))) {
                result.add(item);
            }
        }

        return result;
    }
}
