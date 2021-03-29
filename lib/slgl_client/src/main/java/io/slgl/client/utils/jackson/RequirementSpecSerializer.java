package io.slgl.client.utils.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.slgl.client.node.permission.Aggregate;
import io.slgl.client.node.permission.Op;
import io.slgl.client.node.permission.Requirement;

import java.io.IOException;

import static io.slgl.client.node.permission.Aggregate.noAggregate;

class RequirementSpecSerializer extends StdSerializer<Requirement.Spec> implements ResolvableSerializer {
    private JsonSerializer<Requirement.Spec> delegate;


    public RequirementSpecSerializer(JsonSerializer<Requirement.Spec> delegate) {
        super(Requirement.Spec.class);
        this.delegate = delegate;
    }

    public Requirement.Spec value(Object value) {
        return new Requirement.Spec(Op.EQUAL, null, value, null, null);
    }

    @Override
    public void resolve(SerializerProvider provider) throws JsonMappingException {
        ((ResolvableSerializer) delegate).resolve(provider);
    }

    @Override
    public void serialize(Requirement.Spec spec, JsonGenerator gen, SerializerProvider provider) throws IOException {
        Op<?> op = spec.getOp();
        Aggregate<?> aggregate = spec.getAggregate();
        String var = spec.getVar();
        Object value = spec.getValue();
        if (
                var == null
                        && (aggregate == null || noAggregate().equals(aggregate))
                        && (op == null || Op.EQUAL.equals(op))
                        && isSimpleType(value)
        ) {
            gen.writeObject(value);
        } else {
            delegate.serialize(spec, gen, provider);
        }
    }

    private boolean isSimpleType(Object value) {
        return value == null
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean;
    }
}
