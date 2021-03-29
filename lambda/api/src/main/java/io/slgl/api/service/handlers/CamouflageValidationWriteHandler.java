package io.slgl.api.service.handlers;

import io.slgl.api.ExecutionContext;
import io.slgl.api.camouflage.service.CamouflageValidator;
import io.slgl.api.protocol.NodeRequest;

public class CamouflageValidationWriteHandler implements NodeWriteHandler {

    private final CamouflageValidator camouflageValidator = ExecutionContext.get(CamouflageValidator.class);

    @Override
    public boolean isInterested(NodeRequest object) {
        return object.getCamouflage() != null;
    }

    @Override
    public void validate(NodeRequest obj) {
        camouflageValidator.validate(obj);
    }
}
