package io.slgl.streamprocessor.message;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class Block {

    private String strandId;
    private long sequenceNo;

    private List<String> documentIds;
}
