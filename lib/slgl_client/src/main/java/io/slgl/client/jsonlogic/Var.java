package io.slgl.client.jsonlogic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Objects;

@JsonDeserialize(as = Var.class)
public class Var implements JsonLogic {

    public static final String OP = "var";

    @JsonProperty(OP)
    private final String var;

    @JsonCreator
    public Var(String var) {
        this.var = var;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Var var1 = (Var) o;
        return Objects.equals(var, var1.var);
    }

    @Override
    public int hashCode() {
        return Objects.hash(var);
    }
}
