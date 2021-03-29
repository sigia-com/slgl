package io.slgl.api.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.slgl.api.repository.NodeEntity;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
@JsonPropertyOrder({"@id", "@type", "@state"})
@JsonTypeName("node")
public class NodeResponse implements ApiResponseItem {

    @JsonProperty("@id")
    private String id;

    @JsonProperty("@type")
    private Object type;

    @JsonProperty("@state_source")
    private String stateSource;

    @JsonProperty("@state")
    private Map<String, Object> state;

    private String created;
    private String fileSha3;
    private String objectSha3;
    private String stateSha3;
    private String camouflageSha3;

    public static NodeResponse fromNodeEntity(NodeEntity entity) {
        return new NodeResponse()
                .setId(entity.getId())
                .setType(entity.getType())
                .setStateSource(entity.getStateSource())
                .setCreated(entity.getCreated())
                .setObjectSha3(entity.getObjectSha3())
                .setFileSha3(entity.getFileSha3())
                .setStateSha3(entity.getStateSha3())
                .setCamouflageSha3(entity.getCamouflageSha3());
    }
}
