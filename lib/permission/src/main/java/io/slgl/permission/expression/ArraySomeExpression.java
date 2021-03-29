package io.slgl.permission.expression;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.slgl.permission.PermissionEvaluationLogger;
import io.slgl.permission.context.EvaluationContext;

import java.util.List;

import static java.util.Collections.singletonMap;

public class ArraySomeExpression extends AbstractArrayExpression {

    private final PermissionEvaluationLogger logger;

    public ArraySomeExpression(PermissionEvaluationLogger logger) {
        this.logger = logger;
    }

    @Override
    public String key() {
        return "some";
    }

    @Override
    protected Object evaluateArray(JsonLogicEvaluator evaluator, List<?> array, String itemKey,
                                   JsonLogicNode expression, EvaluationContext context) {


        for (Object item : array) {
            EvaluationContext localContext = EvaluationContext.of(singletonMap(itemKey, item)).withParentContext(context);
            try {
                if (JsonLogic.truthy(evaluator.evaluate(expression, localContext))) {
                    return true;
                }
            } catch (Throwable e) {
                logger.log("some_expression_item_exception",
                        "Evaluation of item in `some` expression thrown exception: " + e.getMessage());
            }
        }

        return false;
    }
}
