package io.slgl.api.service.handlers;

import io.slgl.api.domain.Node;
import io.slgl.api.protocol.NodeRequest;

public interface NodeWriteHandler {

    boolean isInterested(NodeRequest request);

    default void validate(NodeRequest request) {
    }

    default void beforeCommit(Node node) {
    }

    default void afterCommit(Node node) {
    }
}
