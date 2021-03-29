package io.slgl.permission.context;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

class EmptyEvaluationContext implements EvaluationContext {

    @Override
    public Object get(String key) {
        return null;
    }

    @Override
    public Map<String, Object> getAccessedValues() {
        return Collections.emptyMap();
    }

    @Override
    public Optional<EvaluationContext> getNestedContext(String key) {
        return Optional.empty();
    }

    @Override
    public Map<String, Object> asInitializedMap() {
        return Collections.emptyMap();
    }
}
