package io.slgl.api.repository;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class LinkEntity {

    @JsonProperty(value = "id", access = JsonProperty.Access.WRITE_ONLY)
    private String id;

    @JsonProperty("source_node")
    private String sourceNode;

    @JsonProperty("target_node")
    private String targetNode;

    @JsonProperty("target_anchor")
    private String targetAnchor;

    private String created;
}
