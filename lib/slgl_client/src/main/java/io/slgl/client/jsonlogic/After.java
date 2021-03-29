package io.slgl.client.jsonlogic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static io.slgl.client.utils.Preconditions.checkArgument;

@JsonDeserialize(as = After.class)
public class After implements JsonLogic {

    public static final String OP = "after";

    @JsonProperty(OP)
    private List<Object> after;

    @JsonCreator
    After(@JsonProperty(OP) List<Object> list) {
        checkArgument(list.size() == 2);
        after = list;
    }

    public After(Object a, Object b) {
        after = Arrays.asList(a, b);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        After after1 = (After) o;
        return Objects.equals(after, after1.after);
    }

    @Override
    public int hashCode() {
        return Objects.hash(after);
    }
}
