package io.slgl.permission.context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EvaluationContextBuilder {

    private final Map<String, Object> staticValues = new LinkedHashMap<>();
    private final Map<String, Supplier<?>> providers = new LinkedHashMap<>();
    private final List<AbstractEvaluationContext> contexts = new ArrayList<>();

    public EvaluationContextBuilder provider(String key, Supplier<?> supplier) {
        providers.put(key, supplier);
        return this;
    }

    public EvaluationContextBuilder provider(Pattern pattern, Supplier<?> supplier) {
        return provider(pattern, (b, c) -> supplier.get());
    }

    public EvaluationContextBuilder provider(Pattern pattern, BiFunction<Matcher, String, ?> provider) {
        contexts.add(new PatternBasedEvaluationContext(pattern, provider));
        return this;
    }

    public EvaluationContextBuilder value(String key, Object value) {
        if (value instanceof EvaluationContextObject) {
            this.staticValues.put(key, ((EvaluationContextObject) value).asEvaluationContext());
        } else {
            this.staticValues.put(key, value);
        }
        return this;
    }

    public EvaluationContextBuilder values(Map<?, ?> values) {
        if (values != null) {
            values.entrySet().stream()
                    .filter(entry -> entry.getKey() instanceof String)
                    .forEach(entry -> value(((String) entry.getKey()), entry.getValue()));
        }

        return this;
    }

    public EvaluationContext build() {
        ArrayList<EvaluationContext> contexts = new ArrayList<>();
        if (!staticValues.isEmpty()) {
            contexts.add(new StaticEvaluationContext(staticValues));
        }
        contexts.addAll(this.contexts);
        if (!providers.isEmpty()) {
            contexts.add(new SupplierBasedEvaluationContext(providers));
        }
        if (contexts.size() == 1) {
            return contexts.get(0);
        }
        return new CompositeEvaluationContext(contexts);
    }
}
