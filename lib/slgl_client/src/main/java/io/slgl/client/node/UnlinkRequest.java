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

@JsonPropertyOrder({"id", "authorizations"})
@JsonInclude(NON_NULL)
@JsonTypeName("unlink")
public class UnlinkRequest implements WriteRequestItem {

    @JsonProperty("id")
    private final Object id;

    @JsonProperty("authorizations")
    private final List<String> authorizations;

    public UnlinkRequest(Object id, List<String> authorizations) {
        this.id = id;
        this.authorizations = authorizations != null ? Collections.unmodifiableList(new ArrayList<>(authorizations)) : null;
    }

    public Object getId() {
        return id;
    }

    public List<String> getAuthorizations() {
        return authorizations;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder implements WriteRequestItemBuilder {
        private Object id;
        private List<String> authorizations;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder id(Identified link) {
            return id(link.getId());
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
        public UnlinkRequest build() {
            return new UnlinkRequest(id, authorizations);
        }
    }
}
