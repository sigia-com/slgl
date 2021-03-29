package io.slgl.permission.context;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

class SupplierBasedEvaluationContext extends AbstractEvaluationContext {

    private final Map<String, Supplier<?>> providers;

    SupplierBasedEvaluationContext(Map<String, Supplier<?>> providers) {
        this.providers = new LinkedHashMap<>(providers);
    }

    @Override
    protected Object retrieve(String key) {
        Supplier<?> matcher = providers.get(key);
        if (matcher != null) {
            return matcher.get();
        }
        return null;
    }

    @Override
    public Map<String, Object> asInitializedMap() {
        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<String, Supplier<?>> provider : providers.entrySet()) {
            result.put(provider.getKey(), EvaluationContext.deepInitialize(provider.getValue().get()));
        }

        return result;
    }
}
