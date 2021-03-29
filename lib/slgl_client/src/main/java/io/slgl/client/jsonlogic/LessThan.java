package io.slgl.client.jsonlogic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static io.slgl.client.utils.Preconditions.checkArgument;

@JsonDeserialize(as = LessThan.class)
public class LessThan implements JsonLogic {

    public static final String OP = "<";

    @JsonProperty(OP)
    private List<Object> lessThan;


    @JsonCreator
    LessThan(@JsonProperty(OP) List<Object> list) {
        checkArgument(list.size() == 2);
        lessThan = list;
    }

    public LessThan(Object a, Object b) {
        lessThan = Arrays.asList(a, b);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LessThan lessThan1 = (LessThan) o;
        return Objects.equals(lessThan, lessThan1.lessThan);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lessThan);
    }
}
