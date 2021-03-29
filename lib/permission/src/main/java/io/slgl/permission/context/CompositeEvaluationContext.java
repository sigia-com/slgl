package io.slgl.permission.context;

import java.util.*;

class CompositeEvaluationContext implements EvaluationContext {

    private final List<EvaluationContext> composites;

    public CompositeEvaluationContext(EvaluationContext... composites) {
        this(Arrays.asList(composites));
    }

    public CompositeEvaluationContext(Collection<EvaluationContext> composites) {
        this.composites = new ArrayList<>(composites);
    }

    @Override
    public Object get(String key) {
        for (EvaluationContext context : composites) {
            Object value = context.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @Override
    public Map<String, Object> getAccessedValues() {
        Map<String, Object> result = new LinkedHashMap<>();
        // iterate in reverse to preserve priority of keys (first key value to be in result)
        ListIterator<EvaluationContext> iterator = composites.listIterator(composites.size());
        while (iterator.hasPrevious()) {
            EvaluationContext composite = iterator.previous();
            result.putAll(composite.getAccessedValues());
        }
        return result;
    }

    @Override
    public Map<String, Object> asInitializedMap() {
        Map<String, Object> result = new LinkedHashMap<>();

        for (EvaluationContext context : composites) {
            for (Map.Entry<String, Object> entry : context.asInitializedMap().entrySet()) {
                result.computeIfAbsent(entry.getKey(), (key) -> entry.getValue());
            }
        }

        return result;
    }
}
