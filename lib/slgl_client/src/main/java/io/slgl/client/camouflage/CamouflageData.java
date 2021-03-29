package io.slgl.client.camouflage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class CamouflageData extends Camouflage {

    @JsonProperty("camouflaged_type")
    private final Object camouflagedType;

    @JsonCreator
    public CamouflageData(
        @JsonProperty("anchors") Map<String, String> anchors,
        @JsonProperty("fake_anchors") List<String> fakeAnchors,
        @JsonProperty("camouflaged_type") Object camouflagedType) {
        super(anchors, fakeAnchors);
        this.camouflagedType = camouflagedType;
    }

    public Object getCamouflagedType() {
        return camouflagedType;
    }
}
