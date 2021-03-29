package io.slgl.api.repository;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class QldbResult {

    @JsonProperty("documentId")
    private String documentId;
}
