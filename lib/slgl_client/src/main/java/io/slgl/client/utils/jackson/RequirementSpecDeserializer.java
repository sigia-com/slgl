package io.slgl.client.utils.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.slgl.client.node.permission.Op;
import io.slgl.client.node.permission.Requirement;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

class RequirementSpecDeserializer extends StdDeserializer<Requirement.Spec> implements ResolvableDeserializer {
    private JsonDeserializer<?> delegate;


    public RequirementSpecDeserializer(JsonDeserializer<?> delegate) {
        super(Requirement.Spec.class);
        this.delegate = delegate;
    }

    @Override
    public void resolve(DeserializationContext ctxt) throws JsonMappingException {
        ((ResolvableDeserializer) delegate).resolve(ctxt);
    }

    @Override
    public Requirement.Spec deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (Objects.equals(p.currentToken(), JsonToken.START_OBJECT)) {
            return (Requirement.Spec) delegate.deserialize(p, ctxt);
        }
        JsonNode jsonNode = p.readValueAs(JsonNode.class);
        if (jsonNode.isTextual()) {
            return value(jsonNode.textValue());
        }
        if (jsonNode.isIntegralNumber()) {
            return value(jsonNode.intValue());
        }
        if (jsonNode.isFloat()) {
            return value(jsonNode.doubleValue());
        }
        if (jsonNode.isBoolean()) {
            return value(jsonNode.booleanValue());
        }
        if (jsonNode.isArray()) {
            return value(p.readValuesAs(List.class));
        }
        return value(p.readValuesAs(Object.class));
    }

    public Requirement.Spec value(Object value) {
        return new Requirement.Spec(Op.EQUAL, null, value, null, null);
    }
}
