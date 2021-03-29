package io.slgl.client.node;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;
import java.util.stream.Collectors;

public class WriteResponse {

    @JsonProperty("responses")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    private final List<WriteResponseItem> responses;

    @JsonCreator
    public WriteResponse(
            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
            @JsonProperty("responses") List<WriteResponseItem> responses
    ) {
        this.responses = responses;
    }

    public List<WriteResponseItem> getResponses() {
        return responses;
    }

    @JsonIgnore
    public List<NodeResponse> getNodes() {
        return responses.stream()
                .filter(NodeResponse.class::isInstance)
                .map(NodeResponse.class::cast)
                .collect(Collectors.toList());
    }

    @JsonIgnore
    public List<LinkResponse> getLinks() {
        return responses.stream()
                .filter(LinkResponse.class::isInstance)
                .map(LinkResponse.class::cast)
                .collect(Collectors.toList());
    }

    @JsonIgnore
    public List<UnlinkResponse> getUnlinks() {
        return responses.stream()
                .filter(UnlinkResponse.class::isInstance)
                .map(UnlinkResponse.class::cast)
                .collect(Collectors.toList());
    }
}
