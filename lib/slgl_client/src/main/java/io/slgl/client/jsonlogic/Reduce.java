package io.slgl.client.jsonlogic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static io.slgl.client.utils.Preconditions.checkArgument;


@JsonDeserialize(as = Reduce.class)
public class Reduce implements JsonLogic {

    public static final String OP = "reduce";

    @JsonProperty(OP)
    private List<Object> reduce;

    @JsonCreator
    Reduce(@JsonProperty(OP) List<Object> list) {
        checkArgument(list.size() == 3);
        reduce = list;
    }

    public Reduce(Object array, JsonLogic function, Object initial) {
        reduce = Arrays.asList(array, function, initial);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reduce reduce1 = (Reduce) o;
        return Objects.equals(reduce, reduce1.reduce);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reduce);
    }
}
