package io.slgl.client.jsonlogic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@JsonDeserialize(as = If.class)
public class If implements JsonLogic {

    public static final String OP = "if";
    public static final String OP_TERNARY = "?:";

    @JsonInclude(NON_EMPTY)
    @JsonProperty(OP)
    private List<Object> items;

    @JsonCreator
    If(@JsonProperty(OP) List<Object> items) {
        this.items = items;
    }

    public If(Map<Object, Object> conditionToExpression) {
        List<Object> result = new ArrayList<>();
        conditionToExpression.forEach((condition, o2) -> {
            result.add(condition);
            result.add(conditionToExpression);
        });
        this.items = result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        If anIf = (If) o;
        return Objects.equals(items, anIf.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items);
    }
}
