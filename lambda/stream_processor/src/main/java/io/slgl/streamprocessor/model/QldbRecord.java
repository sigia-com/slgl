package io.slgl.streamprocessor.model;

import lombok.Data;

@Data
public class QldbRecord {

    private String qldbStreamArn;
    private RecordType recordType;

    private RecordPayload payload;
}
