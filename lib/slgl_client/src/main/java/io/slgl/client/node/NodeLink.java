package io.slgl.client.node;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.slgl.client.protocol.Identified;
import io.slgl.client.utils.Preconditions;

import java.util.Objects;


public class NodeLink {

    private final String parentId;
    private final String anchor;

    private NodeLink(String parentId, String anchor) {
        this.parentId = Objects.requireNonNull(parentId, "parentId");
        this.anchor = Objects.requireNonNull(anchor, "anchor");
        Preconditions.checkArgument(anchor.startsWith("#"), "anchor should start with #");
    }

    @JsonCreator
    public static NodeLink parse(String linkUrl) {
        if (linkUrl == null || linkUrl.isEmpty()) {
            return null;
        }
        int delimiterIdx = linkUrl.lastIndexOf('#');
        Preconditions.checkArgument(delimiterIdx > 0, "linkUrl has to contain anchor delimiter: `#`");
        return new NodeLink(linkUrl.substring(0, delimiterIdx), linkUrl.substring(delimiterIdx));
    }

    public static NodeLink create(String parent, String anchor) {
        return new NodeLink(parent, anchor);
    }

    public static NodeLink create(Identified parent, String anchor) {
        return new NodeLink(parent.getId(), anchor);
    }

    @JsonValue
    public String fullUrl() {
        return parentId + anchor;
    }

    public String getParentId() {
        return this.parentId;
    }

    public String getAnchor() {
        return this.anchor;
    }

    @Override
    public String toString() {
        return fullUrl();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeLink nodeLink = (NodeLink) o;
        return parentId.equals(nodeLink.parentId) &&
                anchor.equals(nodeLink.anchor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentId, anchor);
    }
}
