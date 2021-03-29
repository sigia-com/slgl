package io.slgl.client.node;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.slgl.client.protocol.Identified;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonPropertyOrder({"source_node", "target_node", "target_anchor", "authorizations"})
@JsonInclude(NON_NULL)
@JsonTypeName("link")
public class LinkRequest implements WriteRequestItem {

    @JsonProperty("source_node")
    private final Object sourceNode;

    @JsonProperty("target_node")
    private final Object targetNode;

    @JsonProperty("target_anchor")
    private final String targetAnchor;

    @JsonProperty("authorizations")
    private final List<String> authorizations;

    public LinkRequest(Object sourceNode, Object targetNode, String targetAnchor, List<String> authorizations) {
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;
        this.targetAnchor = targetAnchor;
        this.authorizations = authorizations != null ? Collections.unmodifiableList(new ArrayList<>(authorizations)) : null;
    }

    public Object getSourceNode() {
        return sourceNode;
    }

    public Object getTargetNode() {
        return targetNode;
    }

    public String getTargetAnchor() {
        return targetAnchor;
    }

    public List<String> getAuthorizations() {
        return authorizations;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder implements WriteRequestItemBuilder {
        private Object sourceNode;
        private Object targetNode;
        private String targetAnchor;
        private List<String> authorizations;

        public Builder sourceNode(String nodeId) {
            sourceNode = nodeId;
            return this;
        }

        public Builder sourceNode(Identified node) {
            return sourceNode(node.getId());
        }

        public Builder sourceNode(int nodeRef) {
            sourceNode = nodeRef;
            return this;
        }

        public Builder targetNode(String nodeId) {
            targetNode = nodeId;
            return this;
        }

        public Builder targetNode(Identified node) {
            return targetNode(node.getId());
        }

        public Builder targetNode(int nodeRef) {
            targetNode = nodeRef;
            return this;
        }

        public Builder targetAnchor(String anchor) {
            targetAnchor = anchor;
            return this;
        }

        public Builder authorizations(List<String> nodeIds) {
            if (authorizations == null) {
                authorizations = new ArrayList<>();
            }

            authorizations.addAll(nodeIds);

            return this;
        }

        public Builder authorization(String nodeId) {
            return authorizations(Collections.singletonList(nodeId));
        }

        public Builder authorization(Identified node) {
            return authorization(node.getId());
        }

        public Builder authorizations(String... nodeIds) {
            return authorizations(Arrays.asList(nodeIds));
        }

        public Builder authorizations(Identified... nodes) {
            return authorizations(Arrays.stream(nodes)
                    .map(node -> node.getId())
                    .collect(Collectors.toList()));
        }

        @Override
        public LinkRequest build() {
            return new LinkRequest(sourceNode, targetNode, targetAnchor, authorizations);
        }
    }
}
