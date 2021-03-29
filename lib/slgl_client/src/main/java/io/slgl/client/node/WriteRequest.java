package io.slgl.client.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.slgl.client.protocol.Identified;

import java.util.*;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

public class WriteRequest {

    @JsonProperty("requests")
    @JsonInclude(NON_EMPTY)
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    private final List<WriteRequestItem> requests;

    @JsonProperty("existing_nodes")
    @JsonInclude(NON_NULL)
    private final ExistingNodes existingNodes;

    public WriteRequest(WriteRequest.Builder builder) {
        this.requests = Collections.unmodifiableList(new ArrayList<>(builder.requests));

        if (!builder.existingNodesState.isEmpty() || !builder.existingNodesRequests.isEmpty()) {
            existingNodes = new ExistingNodes(builder);
        } else {
            existingNodes = null;
        }
    }

    public List<WriteRequestItem> getRequests() {
        return requests;
    }

    public ExistingNodes getExistingNodes() {
        return existingNodes;
    }

    @JsonIgnore
    public Map<String, Object> getExistingNodesState() {
        if (existingNodes == null) {
            return Collections.emptyMap();
        }

        return existingNodes.getState();
    }

    @JsonIgnore
    public Map<String, NodeRequest> getExistingNodesRequests() {
        if (existingNodes == null) {
            return Collections.emptyMap();
        }

        return existingNodes.getRequests();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class ExistingNodes {

        @JsonProperty("state")
        @JsonInclude(NON_EMPTY)
        private final Map<String, Object> state;

        @JsonProperty("requests")
        @JsonInclude(NON_EMPTY)
        private final Map<String, NodeRequest> requests;

        public ExistingNodes(Map<String, Object> state, Map<String, NodeRequest> requests) {
            this.state = state;
            this.requests = requests;
        }

        public ExistingNodes(Builder builder) {
            this.state = Collections.unmodifiableMap(new LinkedHashMap<>(builder.existingNodesState));
            this.requests = Collections.unmodifiableMap(new LinkedHashMap<>(builder.existingNodesRequests));
        }

        public Map<String, Object> getState() {
            return state;
        }

        public Map<String, NodeRequest> getRequests() {
            return requests;
        }
    }

    public static class Builder {

        private List<WriteRequestItem> requests = new ArrayList<>();
        private Map<String, Object> existingNodesState = new LinkedHashMap<>();
        private Map<String, NodeRequest> existingNodesRequests = new LinkedHashMap<>();

        public Builder addRequest(WriteRequestItem request) {
            requests.add(request);
            return this;
        }

        public Builder addRequest(WriteRequestItemBuilder request) {
            return addRequest(request.build());
        }

        public Builder addRequests(WriteRequestItem... requests) {
            return addRequests(Arrays.asList(requests));
        }

        public Builder addRequests(Collection<? extends WriteRequestItem> requests) {
            requests.forEach(this::addRequest);
            return this;
        }

        public Builder addRequests(WriteRequestItemBuilder... requests) {
            for (WriteRequestItemBuilder request : requests) {
                addRequest(request.build());
            }
            return this;
        }

        public Builder addLinkRequest(String sourceNode, String targetNode, String targetAnchor) {
            return addRequests(LinkRequest.builder()
                    .sourceNode(sourceNode)
                    .targetNode(targetNode)
                    .targetAnchor(targetAnchor));
        }

        public Builder addLinkRequest(int sourceNode, String targetNode, String targetAnchor) {
            return addRequests(LinkRequest.builder()
                    .sourceNode(sourceNode)
                    .targetNode(targetNode)
                    .targetAnchor(targetAnchor));
        }

        public Builder addLinkRequest(int sourceNode, Identified targetNode, String targetAnchor) {
            return addRequests(LinkRequest.builder()
                    .sourceNode(sourceNode)
                    .targetNode(targetNode)
                    .targetAnchor(targetAnchor));
        }

        public Builder addLinkRequest(String sourceNode, int targetNode, String targetAnchor) {
            return addRequests(LinkRequest.builder()
                    .sourceNode(sourceNode)
                    .targetNode(targetNode)
                    .targetAnchor(targetAnchor));
        }

        public Builder addLinkRequest(int sourceNode, int targetNode, String targetAnchor) {
            return addRequests(LinkRequest.builder()
                    .sourceNode(sourceNode)
                    .targetNode(targetNode)
                    .targetAnchor(targetAnchor));
        }

        public Builder addLinkRequest(Identified sourceNode, int targetNode, String targetAnchor) {
            return addRequests(LinkRequest.builder()
                    .sourceNode(sourceNode)
                    .targetNode(targetNode)
                    .targetAnchor(targetAnchor));
        }

        public Builder addLinkRequest(Identified sourceNode, Identified targetNode, String targetAnchor) {
            return addLinkRequest(sourceNode.getId(), targetNode.getId(), targetAnchor);
        }

        public Builder addUnlinkRequest(String id) {
            return addRequest(UnlinkRequest.builder()
                    .id(id));
        }

        public Builder addUnlinkRequest(Identified link) {
            return addRequest(UnlinkRequest.builder()
                    .id(link));
        }

        public Builder addExistingNodeState(String nodeId, Object stateObject) {
            existingNodesState.put(nodeId, stateObject);
            return this;
        }

        public Builder addExistingNodeState(Map<String, Object> state) {
            this.existingNodesState.putAll(state);
            return this;
        }

        public Builder addExistingNodeState(Identified node, Object stateObject) {
            return addExistingNodeState(node.getId(), stateObject);
        }

        public Builder addExistingNodeState(NodeResponse node) {
            return addExistingNodeState(node.getId(), node.getState());
        }

        public Builder addExistingNodeRequest(String nodeId, NodeRequest existingNodeRequest) {
            existingNodesRequests.put(nodeId, existingNodeRequest);
            return this;
        }

        public Builder addExistingNodeRequests(Map<String, NodeRequest> existingNodesRequests) {
            this.existingNodesRequests.putAll(existingNodesRequests);
            return this;
        }

        public Builder addExistingNodeRequest(Identified node, NodeRequest existingNodeRequest) {
            return addExistingNodeRequest(node.getId(), existingNodeRequest);
        }

        public WriteRequest build() {
            return new WriteRequest(this);
        }
    }
}
