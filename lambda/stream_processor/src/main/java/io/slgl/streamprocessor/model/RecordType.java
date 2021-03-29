package io.slgl.streamprocessor.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum RecordType {

    @JsonProperty("CONTROL")
    CONTROL,
    @JsonProperty("BLOCK_SUMMARY")
    BLOCK_SUMMARY,
    @JsonProperty("REVISION_DETAILS")
    REVISION_DETAILS
}
