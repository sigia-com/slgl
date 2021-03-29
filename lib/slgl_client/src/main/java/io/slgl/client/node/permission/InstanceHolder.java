package io.slgl.client.node.permission;

import java.util.HashMap;
import java.util.Map;

class InstanceHolder {

    private static final Map<String, BaseOp<?>> opToInstance = new HashMap<>();

    static <T> BaseOp<T> register(String op, Class<? super T> type) {
        BaseOp<T> requirementOp = new BaseOp<>(op, type);
        opToInstance.put(op, requirementOp);
        return requirementOp;
    }

    static NestedRequirementOp registerNested(String op, String collectionOp) {
        NestedRequirementOp requirementOp = new NestedRequirementOp(op, collectionOp);
        opToInstance.put(op, requirementOp);
        return requirementOp;
    }

    public static Op<?> byOp(String op) {
        Op<?> aggregate = opToInstance.get(op);
        if (aggregate == null) {
            throw new IllegalArgumentException("unknown RequirementOp: " + op);
        }
        return aggregate;
    }
}
