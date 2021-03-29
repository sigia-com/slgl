package io.slgl.client.jsonlogic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Objects;

@JsonDeserialize(as = CustomJsonLogic.class)
public class CustomJsonLogic implements JsonLogic {

    @JsonValue
    private final Object jsonLogic;

    @JsonCreator
    public CustomJsonLogic(Object jsonLogic) {
        this.jsonLogic = jsonLogic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomJsonLogic that = (CustomJsonLogic) o;
        return Objects.equals(jsonLogic, that.jsonLogic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jsonLogic);
    }
}
