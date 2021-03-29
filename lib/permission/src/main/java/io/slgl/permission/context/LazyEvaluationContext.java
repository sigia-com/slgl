package io.slgl.permission.context;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

class LazyEvaluationContext implements EvaluationContext {

    private Supplier<? extends EvaluationContext> supplier;
    private EvaluationContext delegate;

    LazyEvaluationContext(Supplier<? extends EvaluationContext> supplier) {
        this.supplier = requireNonNull(supplier);
    }

    @Override
    public Object get(String key) {
        initialize();
        return delegate.get(key);
    }

    @Override
    public Map<String, Object> asInitializedMap() {
        initialize();
        return delegate.asInitializedMap();
    }

    private void initialize() {
        if (supplier != null) {
            delegate = requireNonNull(supplier.get(), "context supplier should never return null");
            supplier = null;
        }
    }

    @Override
    public Map<String, Object> getAccessedValues() {
        if (delegate == null) {
            return Collections.emptyMap();
        }
        return delegate.getAccessedValues();
    }

}
