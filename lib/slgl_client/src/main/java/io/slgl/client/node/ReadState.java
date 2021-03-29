package io.slgl.client.node;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ReadState {

    @JsonProperty("do_not_show")
    NO_STATE(true),
    @JsonProperty("show_fail_on_unauthorized")
    WITH_STATE(false),
    // TODO remove
    @JsonProperty("show_do_not_fail_on_unauthorized")
    WITH_STATE_IF_AUTHORIZED(false);

    private final boolean isNoState;

    ReadState(boolean isNoState) {
        this.isNoState = isNoState;
    }

    public boolean isNoState() {
        return this.isNoState;
    }
}
