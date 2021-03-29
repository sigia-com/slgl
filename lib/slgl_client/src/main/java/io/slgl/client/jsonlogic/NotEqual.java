package io.slgl.client.jsonlogic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static io.slgl.client.utils.Preconditions.checkArgument;


@JsonDeserialize(as = NotEqual.class)
public class NotEqual implements JsonLogic {

    public static final String OP = "!=";

    @JsonProperty(OP)
    private List<Object> notEqual;

    @JsonCreator
    NotEqual(@JsonProperty(OP) List<Object> list) {
        checkArgument(list.size() == 2);
        notEqual = list;
    }

    public NotEqual(Object a, Object b) {
        notEqual = Arrays.asList(a, b);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotEqual notEqual1 = (NotEqual) o;
        return Objects.equals(notEqual, notEqual1.notEqual);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notEqual);
    }
}
