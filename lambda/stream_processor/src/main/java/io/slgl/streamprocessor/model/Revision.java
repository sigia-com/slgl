package io.slgl.streamprocessor.model;

import lombok.Data;

@Data
public class Revision {

    private BlockAddress blockAddress;
    private Object data;
    private RevisionMetadata metadata;
}
