package io.slgl.client.node;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.slgl.client.protocol.Identified;

import java.util.*;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonPropertyOrder({"id", "show_state", "authorizations"})
@JsonInclude(NON_NULL)
public class ReadNodeRequest {

    @JsonProperty("id")
    private final String id;

    @JsonProperty("show_state")
    private final ReadState showState;

    @JsonProperty("authorizations")
    private final List<String> authorizations;

    public ReadNodeRequest(String id, ReadState showState) {
        this(id, showState, null);
    }

    public ReadNodeRequest(String id, ReadState showState, List<String> authorizations) {
        this.id = id;
        this.showState = showState;
        this.authorizations = authorizations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReadNodeRequest that = (ReadNodeRequest) o;
        return Objects.equals(id, that.id) &&
                showState == that.showState &&
                Objects.equals(authorizations, that.authorizations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, showState, authorizations);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String id;
        private ReadState showState;
        private List<String> authorizations;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder id(Identified node) {
            return id(node.getId());
        }

        public Builder showState(ReadState showState) {
            this.showState = showState;
            return this;
        }

        public Builder authorizations(List<String> authorizations) {
            if (this.authorizations == null) {
                this.authorizations = new ArrayList<>();
            }

            this.authorizations.addAll(authorizations);
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

        public ReadNodeRequest build() {
            return new ReadNodeRequest(id, showState, authorizations);
        }
    }
}