package io.slgl.api.service.handlers;

import com.google.common.collect.ImmutableList;
import io.slgl.api.ExecutionContext;
import io.slgl.api.domain.Link;
import io.slgl.api.domain.Node;
import io.slgl.api.domain.Unlink;
import io.slgl.api.protocol.LinkRequest;
import io.slgl.api.protocol.NodeRequest;

import java.util.List;

public class WriteHandlerService {

    private final List<NodeWriteHandler> nodeWriteHandlers = ImmutableList.of(
            ExecutionContext.get(UserWriteHandler.class),
            ExecutionContext.get(CamouflageValidationWriteHandler.class),
            ExecutionContext.get(ObserversValidatorWriteHandler.class)
    );

    private final List<LinkWriteHandler> linkWriteHandlers = ImmutableList.of(
            ExecutionContext.get(KeyWriteHandler.class),
            ExecutionContext.get(UserCreditWriteHandler.class),
            ExecutionContext.get(ObserversValidatorWriteHandler.class)
    );

    private final List<UnlinkWriteHandler> unlinkWriteHandlers = ImmutableList.of(
            ExecutionContext.get(KeyDeletionWriteHandler.class)
    );

    public void validate(NodeRequest request) {
        for (NodeWriteHandler handler : nodeWriteHandlers) {
            if (handler.isInterested(request)) {
                handler.validate(request);
            }
        }
    }

    public void beforeCommit(Node node) {
        for (NodeWriteHandler handler : nodeWriteHandlers) {
            if (handler.isInterested(node.getRequest())) {
                handler.beforeCommit(node);
            }
        }
    }

    public void afterCommit(Node node) {
        for (NodeWriteHandler handler : nodeWriteHandlers) {
            if (handler.isInterested(node.getRequest())) {
                handler.afterCommit(node);
            }
        }
    }

    public void beforeCommit(Link link, LinkRequest request) {
        for (LinkWriteHandler handler : linkWriteHandlers) {
            if (handler.isInterested(request)) {
                handler.beforeCommit(link, request);
            }
        }
    }

    public void afterCommit(Link link, LinkRequest request) {
        for (LinkWriteHandler handler : linkWriteHandlers) {
            if (handler.isInterested(request)) {
                handler.afterCommit(link, request);
            }
        }
    }

    public void beforeCommit(Unlink unlink) {
        for (UnlinkWriteHandler handler : unlinkWriteHandlers) {
            if (handler.isInterested(unlink)) {
                handler.beforeCommit(unlink);
            }
        }
    }

    public void afterCommit(Unlink unlink) {
        for (UnlinkWriteHandler handler : unlinkWriteHandlers) {
            if (handler.isInterested(unlink)) {
                handler.afterCommit(unlink);
            }
        }
    }
}
