package io.slgl.client.node;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public interface NodeData {

    @JsonProperty("@id")
    String getId();

    @JsonProperty("@type")
    NodeTypeId getType();

    @JsonProperty("created")
    String getCreated();

    @JsonProperty("file_sha3")
    String getFileSha3();

    @JsonProperty("object_sha3")
    String getObjectSha3();

    @JsonProperty("state_sha3")
    String getStateSha3();

    @JsonProperty("camouflage_sha3")
    String getCamouflageSha3();

    @JsonProperty("@state")
    Map<String, Object> getState();
}
