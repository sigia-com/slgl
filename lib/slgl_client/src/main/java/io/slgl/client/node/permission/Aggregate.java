package io.slgl.client.node.permission;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class Aggregate<RESULT_TYPE> {

    private static final Map<String, Aggregate<?>> opToInstance = new HashMap<>();

    public static final Aggregate<Number> SUM = new Aggregate<>("sum", Number.class);

    static {
        opToInstance.put(SUM.op, SUM);
        opToInstance.put(null, noAggregate());
        opToInstance.put("", noAggregate());
    }

    public static <T> Aggregate<T> noAggregate() {
        return new Aggregate<>();
    }

    private final String op;
    private final Class<RESULT_TYPE> type;

    private Aggregate(String op, Class<RESULT_TYPE> type) {
        this.op = op;
        this.type = type;
    }

    private Aggregate() {
        this.op = null;
        this.type = null;
    }

    @JsonValue
    public String getAggregate() {
        return op;
    }

    public Class<RESULT_TYPE> getType() {
        return type;
    }

    @JsonCreator
    public static Aggregate<?> byName(String name) {
        Aggregate<?> aggregate = opToInstance.get(name);
        if (aggregate == null) {
            throw new IllegalArgumentException("unknown aggregate: " + name);
        }
        return aggregate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Aggregate<?> aggregate = (Aggregate<?>) o;
        return Objects.equals(op, aggregate.op) &&
                Objects.equals(type, aggregate.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(op, type);
    }
}
