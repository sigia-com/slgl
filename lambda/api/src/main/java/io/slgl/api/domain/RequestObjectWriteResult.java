package io.slgl.api.domain;

import io.slgl.api.protocol.NodeResponse;
import io.slgl.api.repository.NodeEntity;
import lombok.Value;
import lombok.With;

import java.util.LinkedHashMap;
import java.util.Map;

@Value
public class RequestObjectWriteResult {

    NodeEntity nodeEntity;
    @With
    Map<String, Object> state;

    public NodeResponse toResponse() {
        return new NodeResponse()
                .setId(nodeEntity.getId())
                .setType(nodeEntity.getType())
                .setStateSource(nodeEntity.getStateSource())
                .setCreated(nodeEntity.getCreated())
                .setObjectSha3(nodeEntity.getObjectSha3())
                .setFileSha3(nodeEntity.getFileSha3())
                .setStateSha3(nodeEntity.getStateSha3())
                .setCamouflageSha3((nodeEntity.getCamouflageSha3()))
                .setState(state);
    }

    public RequestObjectWriteResult withoutStateLinks() {
        LinkedHashMap<String, Object> strippedState = new LinkedHashMap<>(state);
        strippedState.entrySet().removeIf(it -> it.getKey().startsWith("#"));
        return withState(strippedState);
    }
}
