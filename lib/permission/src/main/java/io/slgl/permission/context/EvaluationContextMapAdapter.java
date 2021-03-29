package io.slgl.permission.context;

import java.util.AbstractMap;
import java.util.Set;

class EvaluationContextMapAdapter extends AbstractMap<String, Object> {

    private final EvaluationContext context;

    EvaluationContextMapAdapter(EvaluationContext context) {
        this.context = context;
    }

    @Override
    public Object get(Object key) {
        if (key instanceof String) {
            return context.get((String) key);
        }
        return null;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        throw new UnsupportedOperationException("Cannot retrieve entrySet out of " + getClass().getSimpleName());
    }
}
