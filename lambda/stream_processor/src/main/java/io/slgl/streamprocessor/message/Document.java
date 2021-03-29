package io.slgl.streamprocessor.message;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Document {

    private String strandId;
    private long sequenceNo;

    private String table;
    private String documentId;

    private Object data;
}
