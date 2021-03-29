package io.slgl.template;


import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

class AccessCheckingMap implements Map<String, Object> {

    private final Map<String, Object> values = new HashMap<>();
    private final Map<String, AccessCheckingMap> nestedMaps = new HashMap<>();

    private final Set<String> notAccessedKeys = new LinkedHashSet<>();

    public AccessCheckingMap(Map<String, ?> delegate) {
        for (Entry<String, ?> entry : delegate.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                AccessCheckingMap nestedMap = new AccessCheckingMap((Map<String, ?>) value);
                nestedMaps.put(key, nestedMap);

                value = nestedMap;
            } else {
                notAccessedKeys.add(key);
            }

            values.put(key, value);
        }
    }

    public AccessCheckingMap ignoreKeys(String... keys) {
        List<String> keyList = asList(keys);
        notAccessedKeys.removeAll(keyList);
        nestedMaps.keySet().removeAll(keyList);
        values.keySet().removeAll(keyList);
        return this;
    }

    public AccessCheckingMap ignoreKeys(Predicate<String> ignorePredicate) {
        notAccessedKeys.removeIf(ignorePredicate);
        nestedMaps.keySet().removeIf(ignorePredicate);
        values.keySet().removeIf(ignorePredicate);
        return this;
    }

    public Set<String> getNotAccessedKeys() {
        Set<String> keys = new LinkedHashSet<>(notAccessedKeys);

        for (Entry<String, AccessCheckingMap> entry : nestedMaps.entrySet()) {
            String key = entry.getKey();
            AccessCheckingMap nestedMap = entry.getValue();

            keys.addAll(prefix(key + ".", nestedMap.getNotAccessedKeys()));
        }

        return keys;
    }

    private Set<String> prefix(String prefix, Set<String> set) {
        return set.stream()
                .map(item -> prefix + item)
                .collect(Collectors.toSet());
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return values.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return values.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        notAccessedKeys.remove(key);
        return values.get(key);
    }

    @Override
    public Set<String> keySet() {
        return values.keySet();
    }

    @Override
    public Collection<Object> values() {
        notAccessedKeys.clear();
        return values.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        notAccessedKeys.clear();
        return values.entrySet();
    }

    @Override
    public Object put(String key, Object value) {
        throw new UnsupportedOperationException("AccessCheckingMap is read-only");
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException("AccessCheckingMap is read-only");
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        throw new UnsupportedOperationException("AccessCheckingMap is read-only");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("AccessCheckingMap is read-only");
    }

    @Override
    public boolean equals(Object o) {
        return values.equals(o);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }
}
