package io.slgl.client.jsonlogic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static io.slgl.client.utils.Preconditions.checkArgument;

@JsonDeserialize(as = Add.class)
public class Add implements JsonLogic {

    public static final String OP = "+";

    @JsonProperty(OP)
    private List<Object> add;

    @JsonCreator
    Add(@JsonProperty(OP) List<Object> list) {
        checkArgument(list.size() == 2);
        add = list;
    }

    public Add(Object a, Object b) {
        add = Arrays.asList(a, b);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Add add1 = (Add) o;
        return Objects.equals(add, add1.add);
    }

    @Override
    public int hashCode() {
        return Objects.hash(add);
    }
}
