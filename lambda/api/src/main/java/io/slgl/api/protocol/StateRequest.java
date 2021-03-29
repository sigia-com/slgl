package io.slgl.api.protocol;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;

import java.util.Map;

@Data
public class StateRequest {

    private Map<String, Object> stateData;

    @JsonAnySetter
    public StateRequest addStateData(String nodeId, Object stateObject) {
        stateData.put(nodeId, stateObject);
        return this;
    }
}
