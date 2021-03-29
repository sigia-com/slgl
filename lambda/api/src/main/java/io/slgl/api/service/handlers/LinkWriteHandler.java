package io.slgl.api.service.handlers;

import io.slgl.api.domain.Link;
import io.slgl.api.protocol.LinkRequest;

public interface LinkWriteHandler {

    boolean isInterested(LinkRequest request);

    default void beforeCommit(Link link, LinkRequest request) {
    }

    default void afterCommit(Link link, LinkRequest request) {
    }
}
