package io.slgl.api.service.handlers;

import io.slgl.api.ExecutionContext;
import io.slgl.api.domain.Link;
import io.slgl.api.observer.service.ObserverValidator;
import io.slgl.api.protocol.LinkRequest;
import io.slgl.api.protocol.NodeRequest;

public class ObserversValidatorWriteHandler implements NodeWriteHandler, LinkWriteHandler {

    private final ObserverValidator observerValidator = ExecutionContext.get(ObserverValidator.class);

    @Override
    public boolean isInterested(NodeRequest object) {
        return true;
    }

    @Override
    public void validate(NodeRequest request) {
        observerValidator.validateInLineObservers(request);
    }

    @Override
    public boolean isInterested(LinkRequest request) {
        return true;
    }

    @Override
    public void beforeCommit(Link link, LinkRequest request) {
        observerValidator.validateObserverAgainstParentObservers(link, request);
    }
}
