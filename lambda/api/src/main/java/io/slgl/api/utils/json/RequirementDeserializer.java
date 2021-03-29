package io.slgl.api.utils.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import io.slgl.api.model.PermissionEntity.Requirement;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Slf4j
public class RequirementDeserializer extends StdDeserializer<Requirement> implements ResolvableDeserializer {
    private final JsonDeserializer<?> delegate;

    public RequirementDeserializer(JsonDeserializer<?> delegate) {
        super(Requirement.class);
        this.delegate = delegate;
    }

    @Override
    public void resolve(DeserializationContext ctxt) throws JsonMappingException {
        ((ResolvableDeserializer) delegate).resolve(ctxt);
    }

    @Override
    public Requirement deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (Objects.equals(p.currentToken(), JsonToken.START_OBJECT)) {
            return (Requirement) delegate.deserialize(p, ctxt);
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
        throw new InvalidFormatException(p, "Encountered unsupported type of requirement value", p.getCurrentValue(), null);
    }

    public Requirement value(Object value) {
        return new Requirement().setValue(value);
    }

}