package io.slgl.client.jsonlogic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static io.slgl.client.utils.Preconditions.checkArgument;

@JsonDeserialize(as = GreaterThanOrEqual.class)
public class GreaterThanOrEqual implements JsonLogic {

    public static final String OP = ">=";

    @JsonProperty(OP)
    private List<Object> greaterThanOrEqual;


    @JsonCreator
    GreaterThanOrEqual(@JsonProperty(OP) List<Object> list) {
        checkArgument(list.size() == 2);
        greaterThanOrEqual = list;
    }

    public GreaterThanOrEqual(Object a, Object b) {
        greaterThanOrEqual = Arrays.asList(a, b);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GreaterThanOrEqual that = (GreaterThanOrEqual) o;
        return Objects.equals(greaterThanOrEqual, that.greaterThanOrEqual);
    }

    @Override
    public int hashCode() {
        return Objects.hash(greaterThanOrEqual);
    }
}
