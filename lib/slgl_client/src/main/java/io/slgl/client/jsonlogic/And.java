package io.slgl.client.jsonlogic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@JsonDeserialize(as = And.class)
public class And implements JsonLogic {

    public static final String OP = "and";

    @JsonInclude(NON_EMPTY)
    @JsonProperty(OP)
    private List<Object> items;

    @JsonCreator
    And(@JsonProperty(OP) List<Object> items) {
        this.items = items;
    }

    public And(Object... items) {
        this.items = new ArrayList<>(Arrays.asList(items));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        And and = (And) o;
        return Objects.equals(items, and.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items);
    }
}
