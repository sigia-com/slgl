package io.slgl.client.node;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.slgl.client.protocol.Identified;

import java.util.Map;


@JsonTypeName("node")
public class NodeResponse implements Identified, NodeData, WriteResponseItem {

    @JsonProperty("@id")
    private final String id;

    @JsonProperty("@type")
    private final NodeTypeId type;

    @JsonProperty("@state_source")
    private final String stateSource;

    @JsonProperty("created")
    private final String created;

    @JsonProperty("file_sha3")
    private final String fileSha3;

    @JsonProperty("object_sha3")
    private final String objectSha3;

    @JsonProperty("state_sha3")
    private final String stateSha3;

    @JsonProperty("camouflage_sha3")
    private final String camouflageSha3;

    @JsonProperty("@state")
    private final Map<String, Object> state;

    @JsonCreator
    public NodeResponse(
            @JsonProperty("@id") String id,
            @JsonProperty("@type") NodeTypeId type,
            @JsonProperty("@state_source") String stateSource,
            @JsonProperty("created") String created,
            @JsonProperty("file_sha3") String fileSha3,
            @JsonProperty("object_sha3") String objectSha3,
            @JsonProperty("state_sha3") String stateSha3,
            @JsonProperty("camouflage_sha3") String camouflageSha3,
            @JsonProperty("@state") Map<String, Object> state
    ) {
        this.id = id;
        this.type = type;
        this.stateSource = stateSource;
        this.created = created;
        this.fileSha3 = fileSha3;
        this.objectSha3 = objectSha3;
        this.stateSha3 = stateSha3;
        this.camouflageSha3 = camouflageSha3;
        this.state = state;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public NodeTypeId getType() {
        return type;
    }

    public String getStateSource() {
        return stateSource;
    }

    @Override
    public String getCreated() {
        return created;
    }

    @Override
    public String getFileSha3() {
        return fileSha3;
    }

    @Override
    public String getObjectSha3() {
        return objectSha3;
    }

    @Override
    public String getStateSha3() {
        return stateSha3;
    }

    @Override
    public String getCamouflageSha3() {
        return camouflageSha3;
    }

    @Override
    public Map<String, Object> getState() {
        return state;
    }
}

