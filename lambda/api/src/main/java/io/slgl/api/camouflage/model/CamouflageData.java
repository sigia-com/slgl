package io.slgl.api.camouflage.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class CamouflageData extends Camouflage {

    private final Object camouflagedType;

    @JsonCreator
    public CamouflageData(
        @JsonProperty("anchors") Map<String, String> anchors,
        @JsonProperty("fake_anchors") List<String> fakeAnchors,
        @JsonProperty("camouflaged_type") Object camouflagedType) {
        super(anchors, fakeAnchors);
        this.camouflagedType = camouflagedType;
    }
}
