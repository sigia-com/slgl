package io.slgl.api.camouflage.service;

import com.google.common.collect.Lists;
import io.slgl.api.ExecutionContext;
import io.slgl.api.camouflage.model.Camouflage;
import io.slgl.api.error.ApiException;
import io.slgl.api.protocol.NodeRequest;
import io.slgl.api.type.Anchor;
import io.slgl.api.type.Type;
import io.slgl.api.type.TypeFactory;
import io.slgl.api.utils.ErrorCode;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CamouflageValidator {

    private final TypeFactory typeFactory = ExecutionContext.get(TypeFactory.class);
    private final List<String> omitAnchors = Lists.newArrayList("#fake");

    public void validate(NodeRequest nodeRequest) {
        if (nodeRequest.getCamouflage() != null) {
            validateCamouflage(nodeRequest.getCamouflage(), getAnchors(nodeRequest));
        }
    }

    private void validateCamouflage(Camouflage camouflage, List<String> anchors) {
        Collection<String> requestAnchors = camouflage.getAnchors().values();
        if (camouflage.getAnchors().keySet().size() != new HashSet<>(requestAnchors).size()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR_WITH_MESSAGE, "Camouflage mapping is not unique");
        }

        if (!(requestAnchors.size() == anchors.size() && anchors.containsAll(requestAnchors))) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR_WITH_MESSAGE, "Camouflage mapping is not consistent with type anchors");
        }
    }


    private List<String> getAnchors(NodeRequest nodeRequest) {
        return Stream.of(typeFactory.get(nodeRequest))
            .map(Type::getAnchors)
            .flatMap(Collection::stream)
            .map(Anchor::getId)
            .filter(anchor -> !omitAnchors.contains(anchor))
            .collect(Collectors.toList());
    }
}
