package io.slgl.client.utils.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.slgl.client.jsonlogic.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JsonLogicDeserializer extends StdDeserializer<JsonLogic> {

    private Map<String, Class<? extends JsonLogic>> opToType = new HashMap<>(40);

    private static final Logger log = LoggerFactory.getLogger(JsonLogicDeserializer.class);

    {
        // base
        opToType.put(And.OP, And.class);
        opToType.put(If.OP, If.class);
        opToType.put(If.OP_TERNARY, If.class);
        opToType.put(Not.OP, Not.class);
        opToType.put(Or.OP, Or.class);
        opToType.put(Var.OP, Var.class);
        opToType.put(VarMap.OP, VarMap.class);

        // compare
        opToType.put(Equal.OP, Equal.class);
        opToType.put(NotEqual.OP, NotEqual.class);

        // numeric compare
        opToType.put(GreaterThan.OP, GreaterThan.class);
        opToType.put(GreaterThanOrEqual.OP, GreaterThanOrEqual.class);
        opToType.put(LessThan.OP, LessThan.class);
        opToType.put(LessThanOrEqual.OP, LessThanOrEqual.class);

        // math
        opToType.put(Add.OP, Add.class);
        opToType.put(Divide.OP, Divide.class);
        opToType.put(Log.OP, Log.class);
        opToType.put(Max.OP, Max.class);
        opToType.put(Min.OP, Min.class);
        opToType.put(Modulo.OP, Modulo.class);
        opToType.put(Multiply.OP, Multiply.class);
        opToType.put(Reduce.OP, Reduce.class);
        opToType.put(Subtract.OP, Subtract.class);

        // date operations
        opToType.put(After.OP, After.class);
        opToType.put(Before.OP, Before.class);
    }


    protected JsonLogicDeserializer() {
        super(JsonLogic.class);
    }

    @Override
    public JsonLogic deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        TreeNode node = p.readValueAsTree();
        ObjectCodec codec = p.getCodec();

        try {
            for (Map.Entry<String, Class<? extends JsonLogic>> opAndClass : opToType.entrySet()) {
                String op = opAndClass.getKey();
                if (node.get(op) != null) {
                    return codec.treeToValue(node, opAndClass.getValue());
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("Cannot deserialize json logic, falling back to {}", CustomJsonLogic.class.getSimpleName(), e);
        }
        return codec.treeToValue(node, CustomJsonLogic.class);
    }
}
