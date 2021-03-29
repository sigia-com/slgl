package io.slgl.permission;

import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.slgl.client.node.permission.Aggregate;
import io.slgl.permission.utils.JsonLogicNodes;

import java.util.function.Function;

class JsonLogicAggregationFactory {

    private static final Function<JsonLogicNode, JsonLogicNode> SUM = expression ->
            JsonLogicNodes.operation("reduce",
                    expression,
                    JsonLogicNodes.operation("+",
                            JsonLogicNodes.variable("$accumulator"),
                            JsonLogicNodes.variable("$current")
                    ),
                    JsonLogicNodes.value(0)
            );

    Function<JsonLogicNode, JsonLogicNode> create(Aggregate<?> aggregation) {
        if (aggregation == null) {
            return Function.identity();
        }
        if (Aggregate.SUM.equals(aggregation)) {
            return SUM;
        }
        throw new IllegalArgumentException("Unsupported aggregation type: " + aggregation);
    }
}
