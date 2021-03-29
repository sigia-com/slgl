package io.slgl.client.jsonlogic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static io.slgl.client.utils.Preconditions.checkArgument;

@JsonDeserialize(as = Log.class)
public class Log implements JsonLogic {

    public static final String OP = "log";

    @JsonProperty(OP)
    private List<Object> log;

    @JsonCreator
    Log(@JsonProperty(OP) List<Object> list) {
        checkArgument(list.size() == 1);
        log = list;
    }

    public Log(Object a) {
        log = Collections.singletonList(a);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Log log1 = (Log) o;
        return Objects.equals(log, log1.log);
    }

    @Override
    public int hashCode() {
        return Objects.hash(log);
    }
}
