package io.slgl.client.jsonlogic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@JsonDeserialize(as = VarMap.class)
public class VarMap implements JsonLogic {

    public static final String OP = "var_map";

    @JsonProperty(OP)
    private final List<Object> varMap;

    @JsonCreator
    public VarMap(@JsonProperty(OP) List<Object> list) {
        varMap = list;
    }

    @JsonCreator
    public VarMap(String path, Object data) {
        this(Arrays.asList(path, data));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VarMap var1 = (VarMap) o;
        return Objects.equals(varMap, var1.varMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(varMap);
    }
}
