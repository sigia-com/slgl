package io.slgl.client.audit;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PermissionEvaluationType {

    @JsonProperty("link_node")
    LINK_NODE,
    @JsonProperty("unlink_node")
    UNLINK_NODE,
    @JsonProperty("read_state")
    READ_STATE,
    @JsonProperty("use_authorization")
    USE_AUTHORIZATION
}
