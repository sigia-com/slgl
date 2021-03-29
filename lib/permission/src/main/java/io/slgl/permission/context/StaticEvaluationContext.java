package io.slgl.permission.context;

import java.util.LinkedHashMap;
import java.util.Map;

class StaticEvaluationContext extends AbstractEvaluationContext {

    private final Map<String, Object> values;

    StaticEvaluationContext(Map<String, ?> values) {
        this.values = new LinkedHashMap<>(values);
    }

    @Override
    protected Object retrieve(String key) {
        return values.get(key);
    }

    @Override
    public Map<String, Object> asInitializedMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            result.computeIfAbsent(key, ignores -> EvaluationContext.deepInitialize(value));
        });
        return result;
    }
}
