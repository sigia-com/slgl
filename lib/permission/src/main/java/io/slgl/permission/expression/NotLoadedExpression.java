package io.slgl.permission.expression;

import io.github.jamsesso.jsonlogic.ast.JsonLogicArray;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpression;

public class NotLoadedExpression implements JsonLogicExpression {

    private final String key;
    private final String message;

    public NotLoadedExpression(String key, String message) {

        this.key = key;
        this.message = message;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public Object evaluate(JsonLogicEvaluator evaluator, JsonLogicArray arguments, Object data) throws JsonLogicEvaluationException {
        throw new JsonLogicEvaluationException(message);
    }
}
