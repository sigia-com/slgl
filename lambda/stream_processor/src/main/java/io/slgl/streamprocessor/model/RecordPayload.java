package io.slgl.streamprocessor.model;

import lombok.Data;

import java.util.List;

@Data
public class RecordPayload {

    private BlockAddress blockAddress;
    private List<RevisionSummary> revisionSummaries;

    private TableInfo tableInfo;
    private Revision revision;
}
