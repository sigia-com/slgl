package io.slgl.client.node.authorization;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum AuthorizeAction {

    @JsonProperty("link_to_anchor")
    LINK_TO_ANCHOR(true),
    @JsonProperty("read_state")
    READ_STATE(false);

    private boolean requireAnchor;

    AuthorizeAction(boolean requireAnchor) {
        this.requireAnchor = requireAnchor;
    }

    public boolean isRequireAnchor() {
        return this.requireAnchor;
    }
}
