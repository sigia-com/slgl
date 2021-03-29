package io.slgl.api.service.handlers;

import io.slgl.api.domain.Unlink;

public interface UnlinkWriteHandler {

    boolean isInterested(Unlink unlink);

    default void beforeCommit(Unlink unlink) {
    }

    default void afterCommit(Unlink unlink) {
    }
}
