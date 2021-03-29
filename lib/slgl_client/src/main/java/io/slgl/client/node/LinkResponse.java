package io.slgl.client.node;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;


@JsonTypeName("link")
public class LinkResponse implements WriteResponseItem {

    @JsonProperty("id")
    private final String id;

    @JsonProperty("source_node")
    private final String sourceNode;

    @JsonProperty("target_node")
    private final String targetNode;

    @JsonProperty("target_anchor")
    private final String targetAnchor;

    @JsonCreator
    public LinkResponse(
            @JsonProperty("id") String id,
            @JsonProperty("source_node") String sourceNode,
            @JsonProperty("target_node") String targetNode,
            @JsonProperty("target_anchor") String targetAnchor
    ) {
        this.id = id;
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;
        this.targetAnchor = targetAnchor;
    }

    public String getId() {
        return id;
    }

    public String getSourceNode() {
        return sourceNode;
    }

    public String getTargetNode() {
        return targetNode;
    }

    public String getTargetAnchor() {
        return targetAnchor;
    }
}
