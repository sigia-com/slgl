package io.slgl.client.node;


import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonSubTypes({
        @JsonSubTypes.Type(value = NodeRequest.class),
        @JsonSubTypes.Type(value = LinkRequest.class),
        @JsonSubTypes.Type(value = UnlinkRequest.class),
})
public interface WriteRequestItem {
}
