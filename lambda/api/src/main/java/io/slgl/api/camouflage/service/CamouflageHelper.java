package io.slgl.api.camouflage.service;

import io.slgl.api.camouflage.model.CamouflageData;
import io.slgl.api.repository.NodeEntity;
import io.slgl.api.type.BuiltinType;

import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Objects.equal;
import static io.slgl.api.utils.json.UncheckedObjectMapper.MAPPER;
import static java.util.Optional.ofNullable;

public class CamouflageHelper {

    public static boolean isNodeCamouflaged(NodeEntity node) {
        return equal(node.getType(), BuiltinType.CAMOUFLAGE.getId());
    }

    public static Optional<CamouflageData> extractCamouflageData(Optional<Map<String, Object>> state) {
        return state
            .flatMap(s -> ofNullable(s.get("@camouflage")))
            .map(o -> MAPPER.convertValue(o, CamouflageData.class));
    }

    public static Optional<CamouflageData> extractCamouflageData(Map<String, Object> state) {
        return extractCamouflageData(ofNullable(state));
    }
}
