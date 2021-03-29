package io.slgl.api.protocol;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.slgl.api.validator.ValidNodeRequestRefs;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class ApiRequest {

    @Valid
    @NotEmpty
    @ValidNodeRequestRefs
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    private List<ApiRequestItem> requests;

    @Valid
    private ExistingNodes existingNodes;

    public ApiRequest addNode(NodeRequest nodeRequest) {
        if (requests == null) {
            requests = new ArrayList<>();
        }

        requests.add(nodeRequest);

        return this;
    }

    public ApiRequest addLink(LinkRequest linkRequest) {
        if (requests == null) {
            requests = new ArrayList<>();
        }

        requests.add(linkRequest);

        return this;
    }

    public ExistingNodes getExistingNodes() {
        if (existingNodes == null) {
            return new ExistingNodes();
        }

        return existingNodes;
    }

    @Data
    @Accessors(chain = true)
    public static class ExistingNodes {

        private Map<String, Map<String, Object>> state;
        private Map<String, NodeRequest> requests;
    }
}
