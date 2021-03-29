package io.slgl.api.domain;

import io.slgl.api.protocol.ApiResponseItem;
import io.slgl.api.protocol.NodeRequest;

import java.util.List;
import java.util.Map;

public interface RequestItemObject {

    default void acknowledgeInCaches() {
    }

    default void validateBeforeTransaction() {
    }

    default void resolveReferences(List<RequestItemObject> alreadyWritten, Map<String, NodeRequest> existingNodesRequests) {
    }

    ApiResponseItem write();

    default void beforeCommit() {
    }

    default void afterCommit() {
    }
}
