package io.slgl.api.type;

import io.slgl.api.ExecutionContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TypeCache implements ExecutionContext.PreExecutionCallback, ExecutionContext.PostExecutionCallback {

    private final Map<String, Type> cache = new HashMap<>();

    public Optional<Type> getType(String typeId) {
        return Optional.ofNullable(cache.get(typeId));
    }

    public void putType(Type type) {
        String typeId = type.getId();

        if (typeId != null) {
            cache.put(typeId, type);
        }
    }

    @Override
    public void beforeExecution() {
        cache.clear();
    }

    @Override
    public void afterExecution() {
        cache.clear();
    }
}
