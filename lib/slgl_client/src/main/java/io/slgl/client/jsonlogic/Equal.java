package io.slgl.client.jsonlogic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static io.slgl.client.utils.Preconditions.checkArgument;


@JsonDeserialize(as = Equal.class)
public class Equal implements JsonLogic {

    public static final String OP = "==";

    @JsonProperty(OP)
    private List<Object> equal;

    @JsonCreator
    Equal(@JsonProperty(OP) List<Object> list) {
        checkArgument(list.size() == 2);
        equal = list;
    }

    public Equal(Object a, Object b) {
        equal = Arrays.asList(a, b);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Equal equal1 = (Equal) o;
        return Objects.equals(equal, equal1.equal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(equal);
    }
}
