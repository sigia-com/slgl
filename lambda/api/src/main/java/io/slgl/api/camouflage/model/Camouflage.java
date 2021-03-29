package io.slgl.api.camouflage.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.slgl.api.error.ApiException;
import io.slgl.api.utils.ErrorCode;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

@Getter
public class Camouflage {

    private final Map<String, String> anchors;
    private final List<String> fakeAnchors;

    @JsonCreator
    public Camouflage(
        @JsonProperty("anchors") Map<String, String> anchors,
        @JsonProperty("fake_anchors") List<String> fakeAnchors) {
        this.anchors = unmodifiableMap(anchors);
        this.fakeAnchors = unmodifiableList(fakeAnchors);
    }

    public String getOriginalAnchor(String anchor) {
        if (getAnchors().containsKey(anchor)) {
            return getAnchors().get(anchor);
        } else if(getFakeAnchors().contains(anchor)) {
            return "#fake";
        } else {
            throw new ApiException(ErrorCode.PERMISSION_DENIED, anchor);
        }
    }

    public String getCamouflagedAnchor(String anchor) {
        return getAnchors().entrySet().stream()
            .filter(entry -> entry.getValue().equals(anchor))
            .map(Entry::getKey)
            .findFirst()
            .orElseThrow(() -> new ApiException(ErrorCode.PERMISSION_DENIED, anchor));
    }

    public boolean isFakeAnchor(String anchor) {
        return getFakeAnchors().contains(anchor);
    }

}
