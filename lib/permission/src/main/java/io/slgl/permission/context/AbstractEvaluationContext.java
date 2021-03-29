package io.slgl.permission.context;

import java.util.LinkedHashMap;
import java.util.Map;

abstract class AbstractEvaluationContext implements EvaluationContext {

    private final Map<String, Object> accessedValues = new LinkedHashMap<>();

    @Override
    public Object get(String key) {
        Object value = accessedValues.get(key);
        if (value != null) {
            return value;
        }
        Object calculated = transform(retrieve(key));
        if (calculated == null) {
            return null;
        }
        this.accessedValues.put(key, calculated);
        return calculated;
    }

    protected abstract Object retrieve(String key);

    private Object transform(Object value) {
        if (value instanceof EvaluationContextObject) {
            return ((EvaluationContextObject) value).asEvaluationContext();
        }
        if (value instanceof Map) {
            return EvaluationContext.of(((Map<?, ?>) value));
        }
        return value;
    }

    @Override
    public Map<String, Object> getAccessedValues() {
        return this.accessedValues;
    }
}
