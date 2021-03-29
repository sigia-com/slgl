package io.slgl.client.jsonlogic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static io.slgl.client.utils.Preconditions.checkArgument;

@JsonDeserialize(as = Not.class)
public class Not implements JsonLogic {

    public static final String OP = "!";

    @JsonProperty(OP)
    private List<Object> not;

    @JsonCreator
    Not(@JsonProperty(OP) List<Object> list) {
        checkArgument(list.size() == 1);
        not = list;
    }

    public Not(Object a) {
        not = Collections.singletonList(a);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Not not1 = (Not) o;
        return Objects.equals(not, not1.not);
    }

    @Override
    public int hashCode() {
        return Objects.hash(not);
    }
}
