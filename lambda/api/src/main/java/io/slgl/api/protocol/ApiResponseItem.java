package io.slgl.api.protocol;


import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonSubTypes({
        @JsonSubTypes.Type(value = NodeResponse.class),
        @JsonSubTypes.Type(value = LinkResponse.class),
        @JsonSubTypes.Type(value = UnlinkResponse.class),
})
public interface ApiResponseItem {
}
