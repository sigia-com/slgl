package io.slgl.client.jsonlogic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static io.slgl.client.utils.Preconditions.checkArgument;

@JsonDeserialize(as = Before.class)
public class Before implements JsonLogic {

    public static final String OP = "before";

    @JsonProperty(OP)
    private List<Object> before;

    @JsonCreator
    Before(@JsonProperty(OP) List<Object> list) {
        checkArgument(list.size() == 2);
        before = list;
    }

    public Before(Object a, Object b) {
        before = Arrays.asList(a, b);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Before before1 = (Before) o;
        return Objects.equals(before, before1.before);
    }

    @Override
    public int hashCode() {
        return Objects.hash(before);
    }
}
