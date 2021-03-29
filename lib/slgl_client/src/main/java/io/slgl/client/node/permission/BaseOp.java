package io.slgl.client.node.permission;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

class BaseOp<VALUE_TYPE> implements Op<VALUE_TYPE> {

    private final String op;
    private final Class<? super VALUE_TYPE> type;

    BaseOp(String op, Class<? super VALUE_TYPE> type) {
        this.op = requireNonNull(op);
        this.type = requireNonNull(type);
    }

    @Override
    public String getOp() {
        return op;
    }

    @Override
    public Class<? super VALUE_TYPE> getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseOp<?> that = (BaseOp<?>) o;
        return op.equals(that.op) &&
                type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(op, type);
    }

    @Override
    public String toString() {
        return "'" + op + '\'' + "(" + type + ')';
    }
}
