package io.slgl.api.protocol;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonPropertyOrder({"id", "sourceNode", "targetNode", "targetAnchor"})
@JsonTypeName("link")
public class LinkResponse implements ApiResponseItem {

    private String id;
    private String sourceNode;
    private String targetNode;
    private String targetAnchor;
}
