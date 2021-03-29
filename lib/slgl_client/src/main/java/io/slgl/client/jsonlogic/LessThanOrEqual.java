package io.slgl.client.jsonlogic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static io.slgl.client.utils.Preconditions.checkArgument;

@JsonDeserialize(as = LessThanOrEqual.class)
public class LessThanOrEqual implements JsonLogic {

    public static final String OP = "<=";

    @JsonProperty(OP)
    private List<Object> lessThanOrEqual;


    @JsonCreator
    LessThanOrEqual(@JsonProperty(OP) List<Object> list) {
        checkArgument(list.size() == 2);
        lessThanOrEqual = list;
    }

    public LessThanOrEqual(Object a, Object b) {
        lessThanOrEqual = Arrays.asList(a, b);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LessThanOrEqual that = (LessThanOrEqual) o;
        return Objects.equals(lessThanOrEqual, that.lessThanOrEqual);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lessThanOrEqual);
    }
}
