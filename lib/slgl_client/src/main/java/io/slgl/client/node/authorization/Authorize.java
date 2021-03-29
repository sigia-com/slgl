package io.slgl.client.node.authorization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.slgl.client.protocol.Identified;

import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static java.util.Objects.requireNonNull;

@JsonInclude(NON_EMPTY)
public class Authorize {

    @JsonProperty("action")
    private final AuthorizeAction action;

    @JsonProperty("node")
    private final String node;

    @JsonProperty("anchor")
    private final String anchor;

    public Authorize(AuthorizeAction action, String node) {
        this(action, node, null);
    }

    @JsonCreator
    public Authorize(
            @JsonProperty("action") AuthorizeAction action,
            @JsonProperty("node") String node,
            @JsonProperty("anchor") String anchor
    ) {
        this.action = requireNonNull(action);
        this.node = requireNonNull(node);
        this.anchor = anchor;
    }

    public AuthorizeAction getAction() {
        return action;
    }

    public String getNode() {
        return node;
    }

    public String getAnchor() {
        return anchor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Authorize allow = (Authorize) o;
        return Objects.equals(action, allow.action) &&
                Objects.equals(node, allow.node) &&
                Objects.equals(anchor, allow.anchor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, node, anchor);
    }

    public static Authorize authorizeLink(String node, String anchor) {
        return new Authorize(AuthorizeAction.LINK_TO_ANCHOR, node, anchor);
    }

    public static Authorize authorizeLink(Identified node, String anchor) {
        return new Authorize(AuthorizeAction.LINK_TO_ANCHOR, node.getId(), anchor);
    }

    public static Authorize authorizeReadState(String node) {
        return new Authorize(AuthorizeAction.READ_STATE, node);
    }

    public static Authorize authorizeReadState(Identified node) {
        return new Authorize(AuthorizeAction.READ_STATE, node.getId());
    }
}
