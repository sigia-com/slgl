package io.slgl.client.audit;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum RequestType {

    @JsonProperty("write_node")
    WRITE_NODE,
    @JsonProperty("link_node")
    LINK_NODE,
    @JsonProperty("read_state")
    READ_STATE,
    @JsonProperty("use_authorization")
    USE_AUTHORIZATION
}
