package io.slgl.permission.utils;

import io.github.jamsesso.jsonlogic.ast.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;

public class JsonLogicNodes {

    private JsonLogicNodes() {
    }

    public static JsonLogicOperation operation(String operator, JsonLogicNode... arguments) {
        return new JsonLogicOperation(operator, new JsonLogicArray(asList(arguments)));
    }

    public static JsonLogicOperation operation(String operator, List<JsonLogicNode> arguments) {
        return new JsonLogicOperation(operator, array(arguments));
    }

    public static JsonLogicArray array(List<JsonLogicNode> nodes) {
        return new JsonLogicArray(Collections.unmodifiableList(new ArrayList<>(nodes)));
    }

    public static JsonLogicNode joinWithAnd(JsonLogicNode... nodes) {
        return joinWithAnd(asList(nodes));
    }

    public static JsonLogicNode joinWithAnd(List<JsonLogicNode> nodes) {
        if (nodes == null || nodes.size() == 0) {
            return null;
        }

        if (nodes.size() == 1) {
            return nodes.get(0);
        }

        return operation("and", nodes);
    }

    public static JsonLogicVariable variable(String ref) {
        return new JsonLogicVariable(value(ref), JsonLogicNull.NULL);
    }

    public static JsonLogicNode value(int value) {
        return new JsonLogicNumber(value);
    }

    public static JsonLogicString value(String value) {
        return new JsonLogicString(value);
    }
}
