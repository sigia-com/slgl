package io.slgl.api.authorization.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

public enum AuthorizeAction {

    @JsonProperty("link_to_anchor")
    LINK_TO_ANCHOR(true),
    @JsonProperty("unlink_from_anchor")
    UNLINK_FROM_ANCHOR(true),
    @JsonProperty("read_state")
    READ_STATE(false);

    @Getter
    private final boolean requireAnchor;

    AuthorizeAction(boolean requireAnchor) {
        this.requireAnchor = requireAnchor;
    }
}
