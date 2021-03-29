package io.slgl.permission;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jamsesso.jsonlogic.ast.*;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;
import io.slgl.client.node.permission.Op;
import io.slgl.client.node.permission.Requirement;
import io.slgl.client.node.permission.Requirements;
import io.slgl.client.utils.jackson.ObjectMapperFactory;
import io.slgl.permission.utils.JsonLogicNodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RequirementsProcessor {
    private final JsonLogicAggregationFactory aggregationFactory = new JsonLogicAggregationFactory();
    private final ObjectMapper mapper = ObjectMapperFactory.createSlglObjectMapper();

    public List<JsonLogicNode> convertRequirementsToJsonLogic(Requirements requirements) throws JsonLogicParseException {
        try {
            return convertToJsonLogic(requirements);
        } catch (Exception e) {
            throw new JsonLogicParseException("Exception during requirements to json logic conversion", e);
        }
    }

    private List<JsonLogicNode> convertToJsonLogic(Requirements requirements) throws Exception {
        if (requirements == null) {
            return Collections.emptyList();
        }

        List<JsonLogicNode> result = new ArrayList<>();
        for (Requirement pathRequirement : requirements) {
            result.add(convertRequirement(pathRequirement.getPath(), pathRequirement.getRequirement()));
        }
        return result;
    }

    private JsonLogicNode convertRequirement(String path, Requirement.Spec requirement) throws Exception {
        JsonLogicNode left = aggregationFactory.create(requirement.getAggregate())
                .apply(convertPathExpressionToJsonLogic(path));

        if (requirement.getOp() instanceof Op.NestedRequirementsOp) {
            Op.NestedRequirementsOp op = (Op.NestedRequirementsOp) requirement.getOp();
            if (requirement.getVar() != null) {
                throw new JsonLogicParseException("Requirement operation '" + op + "' can't be used with 'var' parameter");
            }

            Requirements wrapper = mapper.convertValue(requirement.getValue(), Requirements.class);
            JsonLogicNode right = JsonLogicNodes.joinWithAnd(convertToJsonLogic(wrapper));

            String operation = op.getCollectionOperation();
            if (requirement.getAs() != null) {
                return JsonLogicNodes.operation(operation, left, JsonLogicNodes.value(requirement.getAs()), right);
            } else {
                return JsonLogicNodes.operation(operation, left, right);
            }

        } else {

            JsonLogicNode right = requirement.getVar() != null
                    ? convertPathExpressionToJsonLogic(requirement.getVar())
                    : convertValue(requirement.getValue());
            return JsonLogicNodes.operation(requirement.getOp().getOp(), left, right);
        }
    }

    private JsonLogicNode convertValue(Object value) throws JsonProcessingException {
        if (value == null) {
            return JsonLogicNull.NULL;
        }
        if (value instanceof String) {
            return new JsonLogicString((String) value);
        }
        if (value instanceof Number) {
            return new JsonLogicNumber((Number) value);
        }
        if (value instanceof Boolean) {
            return new JsonLogicBoolean((Boolean) value);
        }
        if (ArrayLike.isEligible(value)) {
            List<JsonLogicNode> nodes = convertArray(new ArrayLike(value));
            return new JsonLogicArray(nodes);
        }
        String json = mapper.writeValueAsString(value);
        return JsonLogicNodes.operation("constant", JsonLogicNodes.value(json));
    }

    private List<JsonLogicNode> convertArray(ArrayLike array) throws JsonProcessingException {
        List<JsonLogicNode> result = new ArrayList<>(array.size());
        for (Object item : array) {
            result.add(convertValue(item));
        }
        return result;
    }

    private JsonLogicNode convertPathExpressionToJsonLogic(String path) {
        return JsonLogicNodes.operation("var_map", JsonLogicNodes.value(path));
    }
}
