package io.slgl.api.protocol;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.slgl.api.domain.RequestItemObject;
import io.slgl.api.service.RequestItemObjectFactory;
import io.slgl.api.validator.ValidAnchor;
import io.slgl.api.validator.ValidNodeId;
import io.slgl.api.validator.ValidationProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
@JsonTypeName("link")
public class LinkRequest implements ApiRequestItem {

    @ValidNodeId
    @JsonIgnore
    private String sourceNode;

    @ValidationProperty("source_node")
    @Min(value = 0)
    @JsonIgnore
    private Integer sourceNodeRef;

    @ValidNodeId
    @JsonIgnore
    private String targetNode;

    @ValidationProperty("target_node")
    @Min(value = 0)
    @JsonIgnore
    private Integer targetNodeRef;

    @NotNull
    @ValidAnchor
    private String targetAnchor;

    @JsonProperty("authorizations")
    private List<String> authorizations;

    @JsonSetter("source_node")
    public LinkRequest setSourceNode(Object sourceNode) {
        if (sourceNode instanceof String) {
            this.sourceNode = (String) sourceNode;
        } else if (sourceNode instanceof Integer) {
            this.sourceNodeRef = (Integer) sourceNode;
        } else {
            throw new IllegalArgumentException("sourceNode must be String or Integer");
        }

        return this;
    }

    @JsonSetter("target_node")
    public LinkRequest setTargetNode(Object targetNode) {
        if (targetNode instanceof String) {
            this.targetNode = (String) targetNode;
        } else if (targetNode instanceof Integer) {
            this.targetNodeRef = (Integer) targetNode;
        } else {
            throw new IllegalArgumentException("targetNode must be String or Integer");
        }

        return this;
    }

    @Override
    public RequestItemObject createWriteObject(RequestItemObjectFactory factory) {
        return factory.writeObjectFor(this);
    }
}
