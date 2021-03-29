package io.slgl.client.jsonlogic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static io.slgl.client.utils.Preconditions.checkArgument;

@JsonDeserialize(as = GreaterThan.class)
public class GreaterThan implements JsonLogic {

    public static final String OP = ">";

    @JsonProperty(OP)
    private List<Object> greaterThan;


    @JsonCreator
    GreaterThan(@JsonProperty(OP) List<Object> list) {
        checkArgument(list.size() == 2);
        greaterThan = list;
    }

    public GreaterThan(Object a, Object b) {
        greaterThan = Arrays.asList(a, b);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GreaterThan that = (GreaterThan) o;
        return Objects.equals(greaterThan, that.greaterThan);
    }

    @Override
    public int hashCode() {
        return Objects.hash(greaterThan);
    }
}
