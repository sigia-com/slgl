package io.slgl.permission.context;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public interface EvaluationContext {

    Object get(String key);

    @JsonValue
    Map<String, Object> getAccessedValues();

    default Optional<EvaluationContext> getNestedContext(String key) {
        Object object = get(key);

        if (!(object instanceof EvaluationContext)) {
            return Optional.empty();
        }

        return Optional.of((EvaluationContext) object);
    }

    Map<String, Object> asInitializedMap();

    default EvaluationContext withParentContext(EvaluationContext evaluationContext) {
        return new CompositeEvaluationContext(this, evaluationContext);
    }


    @SuppressWarnings("unchecked")
    static EvaluationContext of(Map<?, ?> ctx) {
        for (Object o : ctx.keySet()) {
            if (!(o instanceof String)) {
                throw new IllegalArgumentException("Evaluation context can only have String keys");
            }
        }
        return new StaticEvaluationContext(((Map<String, ?>) ctx));
    }

    static EvaluationContext wrap(Object context) {
        if (context instanceof EvaluationContext) {
            return ((EvaluationContext) context);
        }
        if (context instanceof Map) {
            //noinspection unchecked
            return new StaticEvaluationContext(((Map<String, ?>) context));
        }
        throw new IllegalArgumentException("could not wrap " + context + " as evaluation context");
    }

    static EvaluationContext lazy(Supplier<? extends EvaluationContext> supplier) {
        return new LazyEvaluationContext(supplier);
    }

    static EvaluationContextBuilder builder() {
        return new EvaluationContextBuilder();
    }

    static Object deepInitialize(Object object) {
        if (object instanceof EvaluationContext) {
            return ((EvaluationContext) object).asInitializedMap();
        }
        if (object instanceof EvaluationContextObject) {
            return deepInitialize(((EvaluationContextObject) object).asEvaluationContext());
        }
        if (object instanceof Collection) {
            return ((Collection<?>) object).stream()
                    .map(EvaluationContext::deepInitialize)
                    .collect(toList());
        }
        if (object instanceof Object[]) {
            return Arrays.stream((Object[]) object)
                    .map(EvaluationContext::deepInitialize)
                    .collect(toList());
        }
        if (object instanceof Map) {
            return ((Map<?, ?>) object).entrySet().stream()
                    .collect(toMap(
                            Map.Entry::getKey,
                            it -> deepInitialize(it.getValue())
                    ));
        }
        return object;
    }
}
