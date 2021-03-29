package io.slgl.client.camouflage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

public class Camouflage {

    @JsonProperty("anchors")
    private final Map<String, String> anchors;
    @JsonProperty("fake_anchors")
    private final List<String> fakeAnchors;

    @JsonCreator
    public Camouflage(
        @JsonProperty("anchors") Map<String, String> anchors,
        @JsonProperty("fake_anchors")List<String> fakeAnchors) {
        this.anchors = anchors;
        this.fakeAnchors = fakeAnchors;
    }

    private  Camouflage(Builder builder) {
        this.anchors = builder.anchors;
        this.fakeAnchors = builder.fakeAnchors;
    }

    public static class Builder {
        private final Map<String, String> anchors = new HashMap<>();
        private final List<String> fakeAnchors = new ArrayList<>();

        public Builder anchor(String camouflaged, String orginal) {
            anchors.put(camouflaged, orginal);
            return this;
        }

        public Builder fakeAnchor(String fake) {
            fakeAnchors.add(fake);
            return this;
        }

        public Builder fakeAnchors(String... fakes) {
            fakeAnchors.addAll(Arrays.asList(fakes));
            return this;
        }

        public Builder fakeAnchors(List<String> fakes) {
            fakeAnchors.addAll(fakes);
            return this;
        }

        public Camouflage build() {
            return new Camouflage(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, String> getAnchors() {
        return anchors;
    }

    public List<String> getFakeAnchors() {
        return fakeAnchors;
    }

    public String getOriginalAnchor(String anchor) {
        if (getAnchors().containsKey(anchor)) {
            return getAnchors().get(anchor);
        } else if(getFakeAnchors().contains(anchor)) {
            return "#fake";
        } else {
            return null;
        }
    }
}
