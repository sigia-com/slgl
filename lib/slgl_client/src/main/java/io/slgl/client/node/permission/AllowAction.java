package io.slgl.client.node.permission;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum AllowAction {
    @JsonProperty("all")
    ALL(false),
    @JsonProperty("link_to_anchor")
    LINK_TO_ANCHOR(true),
    @JsonProperty("unlink_from_anchor")
    UNLINK_FROM_ANCHOR(true),
    @JsonProperty("read_state")
    READ_STATE(false);

    private boolean requireAnchor;

    AllowAction(boolean requireAnchor) {
        this.requireAnchor = requireAnchor;
    }

    public boolean isRequireAnchor() {
        return this.requireAnchor;
    }
}
