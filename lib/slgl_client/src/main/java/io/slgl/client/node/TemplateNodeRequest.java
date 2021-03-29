package io.slgl.client.node;

import io.slgl.client.Types;

public class TemplateNodeRequest extends NodeRequest {
    protected TemplateNodeRequest(Builder builder) {
        super(builder);
    }

    public static Builder template(String text) {
        return new Builder().text(text);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends NodeRequest.Builder<Builder> {

        public Builder() {
            super.type(Types.TEMPLATE);
        }

        public Builder text(String text) {
            data("text", text);
            return this;
        }

        public Builder unorderedListMarkerPattern(String pattern) {
            data("unordered_list_marker_pattern", pattern);
            return this;
        }

        @Override
        public TemplateNodeRequest build() {
            return new TemplateNodeRequest(this);
        }

    }
}
