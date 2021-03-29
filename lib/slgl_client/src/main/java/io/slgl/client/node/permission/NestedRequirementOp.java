package io.slgl.client.node.permission;

import java.util.Map;

import static java.util.Objects.requireNonNull;

class NestedRequirementOp extends BaseOp<Map<String, Requirement.Spec>> implements Op.NestedRequirementsOp {

    private final String collectionOperation;

    NestedRequirementOp(String op, String collectionOp) {
        super(op, Map.class);
        this.collectionOperation = requireNonNull(collectionOp);
    }

    @Override
    public String getCollectionOperation() {
        return collectionOperation;
    }
}
