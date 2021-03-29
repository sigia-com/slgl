package io.slgl.client.node.permission;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static java.util.Objects.requireNonNull;

@JsonInclude(NON_EMPTY)
public class Allow {

    @JsonProperty("action")
    private final AllowAction action;

    @JsonProperty("anchor")
    private final String anchor;


    public Allow(AllowAction action) {
        this(action, null);
    }

    @JsonCreator
    public Allow(
            @JsonProperty("action") AllowAction action,
            @JsonProperty("anchor") String anchor
    ) {
        this.action = requireNonNull(action);
        this.anchor = anchor == null || anchor.isEmpty()
                ? null
                : anchor;
    }

    public AllowAction getAction() {
        return action;
    }

    public String getAnchor() {
        return anchor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Allow allow = (Allow) o;
        return Objects.equals(action, allow.action) &&
                Objects.equals(anchor, allow.anchor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, anchor);
    }

    public static Allow allowLink(String anchor) {
        return new Allow(AllowAction.LINK_TO_ANCHOR, anchor);
    }

    public static Allow allowUnlink(String anchor) {
        return new Allow(AllowAction.UNLINK_FROM_ANCHOR, anchor);
    }

    public static Allow allowReadState() {
        return new Allow(AllowAction.READ_STATE);
    }

    public static Allow allowAll() {
        return new Allow(AllowAction.ALL);
    }
}
