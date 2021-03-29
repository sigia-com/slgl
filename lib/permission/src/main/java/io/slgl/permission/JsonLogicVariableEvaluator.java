package io.slgl.permission;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;
import io.slgl.client.utils.jackson.ObjectMapperFactory;
import io.slgl.permission.context.EvaluationContext;
import io.slgl.permission.context.EvaluationContextObject;
import io.slgl.permission.context.FirstAndLastElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.function.Supplier;

public class JsonLogicVariableEvaluator {

    private final ObjectMapper valueMapper = ObjectMapperFactory.createSlglObjectMapper();

    private final boolean autoMap;
    private final PermissionEvaluationLogger logger;

    public JsonLogicVariableEvaluator(boolean autoMap, PermissionEvaluationLogger logger) {
        this.logger = logger;
        this.autoMap = autoMap;
    }

    public Object evaluateVariable(String path, Object data, Object defaultValue) {
        if (path.isEmpty()) {
            return data;
        }
        logger.log("variable_evaluation_started", path);
        try {
            Object evaluated = doEvaluate(path, data);
            Object result = evaluated != null ? evaluated : defaultValue;
            logger.logDynamic("variable_evaluation_result", resultLogValue(path, result));
            return result;
        } catch (Throwable e) {
            logger.log("variable_evaluation_failed", evaluationFailedLogDetails(path, e));
            throw new EvaluationAbortedException(e);
        }
    }

    public Supplier<String> resultLogValue(String path, Object result) {
        return () -> {
            try {
                return String.format("%s\n-> %s", path, valueMapper.writeValueAsString(result));
            } catch (Exception e) {
                return String.format("%s -> Failed to jsonify value: %s", path, e.getMessage());
            }
        };
    }

    public String evaluationFailedLogDetails(String path, Throwable e) {
        return String.format("%s (during evaluation of %s)", e.getMessage(), path);
    }

    public Object doEvaluate(String path, Object data) throws JsonLogicEvaluationException {
        List<String> pathVariables = getPathVariables(path, data);
        Object partialResult = data;
        for (String variable : pathVariables) {
            partialResult = evaluatePartialVariable(variable, partialResult);
        }
        return partialResult;
    }

    private List<String> getPathVariables(String path, Object data) throws JsonLogicEvaluationException {
        List<String> pathVariables = new ArrayList<>();
        PrimitiveIterator.OfInt codePoints = path.chars().iterator();
        while (codePoints.hasNext()) {
            String variable = readVariable(data, codePoints);
            pathVariables.add(variable);
        }
        return pathVariables;
    }

    private String readVariable(Object data, PrimitiveIterator.OfInt iterator) throws JsonLogicEvaluationException {
        return readUntil(data, iterator, '.', false);
    }

    private String readUntil(Object data, PrimitiveIterator.OfInt iterator, char variableEndCharacter, boolean throwOnNoEnd) throws JsonLogicEvaluationException {
        StringBuilder variable = new StringBuilder();
        boolean escaped = false;
        while (iterator.hasNext()) {
            int codePoint = iterator.nextInt();

            // just append on escape
            if (escaped) {
                escaped = false;
                variable.appendCodePoint(codePoint);
                continue;
            }

            // end on the end
            if (codePoint == (int) variableEndCharacter) {
                return variable.toString();

            }

            switch (codePoint) {
                // handle function call
                case (int) '(': {
                    String argument = readUntil(data, iterator, ')', true);
                    Object evaluated = evaluateArgument(data, argument);
                    variable.append('(').append(evaluated).append(')');
                    break;
                }
                // handle escape char
                case (int) '\\': {
                    escaped = true;
                    break;
                }
                default: {
                    variable.appendCodePoint(codePoint);
                    break;
                }
            }
        }

        if (escaped) {
            throw new JsonLogicEvaluationException("Unexpected end of input after escape character `\\`, already read:`" + variable + "`");
        }
        if (throwOnNoEnd) {
            throw new JsonLogicEvaluationException("Unexpected end of input, expecting `" + variableEndCharacter + "`, already read:`" + variable + "`");
        }

        return variable.toString();
    }

    private Object evaluateArgument(Object data, String argument) throws JsonLogicEvaluationException {
        if (argument.startsWith("'")) {
            if (argument.endsWith("'")) {
                return argument;
            }
            throw new JsonLogicEvaluationException("String argument not closed with `'`");
        }
        Object evaluated = evaluateVariable(argument, data, null);
        if (evaluated instanceof String) {
            return "'" + evaluated + "'";
        }
        throw new JsonLogicEvaluationException("Function argument expression (" + argument + ") evaluated to unsupported type");
    }

    private Object evaluatePartialVariable(String key, Object data) throws JsonLogicEvaluationException {
        if (ArrayLike.isEligible(data)) {
            ArrayLike list = new ArrayLike(data);

            if (key.equals("length") || key.equals("$length")) {
                return list.size();
            }

            if (key.equals("$first") || key.equals("$oldest")) {
                if (data instanceof FirstAndLastElement) {
                    return transform(((FirstAndLastElement) data).getFirstElement());
                } else {
                    return !list.isEmpty() ? transform(list.get(0)) : null;
                }
            }
            if (key.equals("$last") || key.equals("$newest")) {
                if (data instanceof FirstAndLastElement) {
                    return transform(((FirstAndLastElement) data).getLastElement());
                } else {
                    return !list.isEmpty() ? transform(list.get(list.size() - 1)) : null;
                }
            }

            if (key.matches("^[0-9]+$")) {
                int index = Integer.parseInt(key);
                return index < list.size() ? transform(list.get(index)) : null;
            }

            if (autoMap) {
                return evaluateMap(key, list);
            } else {
                throw new JsonLogicEvaluationException("Could not get key: `" + key + "` out of array");
            }

        }

        if (data instanceof EvaluationContext) {
            return transform(((EvaluationContext) data).get(key));
        }

        if (data instanceof EvaluationContextObject) {
            return evaluatePartialVariable(key, ((EvaluationContextObject) data)
                    .asEvaluationContext());
        }

        if (data instanceof Map) {
            return transform(((Map<?, ?>) data).get(key));
        }

        return null;
    }

    private List<Object> evaluateMap(String key, ArrayLike list) throws JsonLogicEvaluationException {
        List<Object> resultList = new ArrayList<>();

        for (Object object : list) {
            Object result = evaluatePartialVariable(key, object);
            if (ArrayLike.isEligible(result)) {
                resultList.addAll(new ArrayLike(result));
            } else if (result != null) {
                resultList.add(result);
            }
        }

        return resultList;
    }

    private Object transform(Object value) {
        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }

        return value;
    }
}
