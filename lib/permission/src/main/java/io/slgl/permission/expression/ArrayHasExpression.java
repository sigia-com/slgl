package io.slgl.permission.expression;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.slgl.permission.context.EvaluationContext;

import java.util.List;

import static java.util.Collections.singletonMap;

public class ArrayHasExpression extends AbstractArrayExpression {

    public static final ArrayHasExpression NONE = new ArrayHasExpression("none", true, false);
    public static final ArrayHasExpression ALL = new ArrayHasExpression("all", false, false);

    private final String key;
    private final boolean whenElementEvaluatesTo;
    private final boolean thenReturn;

    private ArrayHasExpression(String key, boolean whenFirstElementIs, boolean thenReturn) {
        this.key = key;
        this.whenElementEvaluatesTo = whenFirstElementIs;
        this.thenReturn = thenReturn;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    protected Object evaluateArray(JsonLogicEvaluator evaluator, List<?> array, String itemKey,
                                   JsonLogicNode expression, EvaluationContext context)
            throws JsonLogicEvaluationException {

        for (Object item : array) {
            EvaluationContext localContext = EvaluationContext.of(singletonMap(itemKey, item)).withParentContext(context);
            if (JsonLogic.truthy(evaluator.evaluate(expression, localContext)) == whenElementEvaluatesTo) {
                return thenReturn;
            }
        }

        return !thenReturn;
    }
}
