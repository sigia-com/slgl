package io.slgl.api.repository;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class LinkIndexEntity {

    @JsonProperty(value = "id", access = JsonProperty.Access.WRITE_ONLY)
    private String id;

    @JsonProperty("link_id")
    private String linkId;
    
    @JsonProperty("sn_tn_ta")
    private String sourceNodeTargetNodeTargetAnchor;

    @JsonProperty("sn_ta")
    private String sourceNodeTargetAnchor;

    @JsonProperty("tn_ta")
    private String targetNodeTargetAnchor;

    @JsonProperty("tn_ta_first")
    private String targetNodeTargetAnchorFirst;

    @JsonProperty("tn_ta_last")
    private String targetNodeTargetAnchorLast;

    @JsonProperty("tn_ta_previous_id")
    private String targetNodeTargetAnchorPreviousId;

    @JsonProperty("tn_ta_next_id")
    private String targetNodeTargetAnchorNextId;
}
