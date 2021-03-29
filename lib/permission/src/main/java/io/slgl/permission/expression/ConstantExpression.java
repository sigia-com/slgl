package io.slgl.permission.expression;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.expressions.PreEvaluatedArgumentsExpression;
import io.slgl.client.utils.jackson.ObjectMapperFactory;

import java.util.List;

public class ConstantExpression implements PreEvaluatedArgumentsExpression {

    private final ObjectMapper mapper = ObjectMapperFactory.createSlglObjectMapper();

    @Override
    public String key() {
        return "constant";
    }

    @Override
    public Object evaluate(List arguments, Object data) throws JsonLogicEvaluationException {
        if (arguments.isEmpty() || !(arguments.get(0) instanceof String)) {
            return null;
        }
        try {
            return mapper.readTree(((String) arguments.get(0)));
        } catch (JsonProcessingException e) {
            throw new JsonLogicEvaluationException("Error during evaluating constant[" + arguments.get(0) + "]", e);
        }
    }
}
