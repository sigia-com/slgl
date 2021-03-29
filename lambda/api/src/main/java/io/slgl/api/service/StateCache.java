package io.slgl.api.service;

import io.slgl.api.ExecutionContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class StateCache implements ExecutionContext.PreExecutionCallback, ExecutionContext.PostExecutionCallback {

    private final Map<String, Map<String, Object>> cache = new HashMap<>();
    private final Map<String, Map<String, Object>> stateFromRequest = new HashMap<>();

    public Optional<Map<String, Object>> get(String nodeId) {
        return Optional.ofNullable(cache.get(nodeId));
    }

    public void put(String nodeId, Map<String, Object> state) {
        cache.put(nodeId, state);
    }

    @Override
    public void beforeExecution() {
        clearCache();
    }

    @Override
    public void afterExecution() {
        clearCache();
    }

    private void clearCache() {
        cache.clear();
    }
}
