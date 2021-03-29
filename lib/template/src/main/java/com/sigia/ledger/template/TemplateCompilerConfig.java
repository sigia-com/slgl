package io.slgl.template;

import java.util.Objects;

class TemplateCompilerConfig {

    static final TemplateCompilerConfig DEFAULT_CONFIG = new TemplateCompilerConfig();

    private static final String DEFAULT_UNORDERED_LIST_MARKER_REGEX = "[\\-+*\\u25A0-\\u25FF\\u2756\\u27A2\\u2794\\u274F\\u2605,\\u2022]?";

    private final String unorderedListMarkerPattern;

    TemplateCompilerConfig(Template template) {
        this.unorderedListMarkerPattern = Objects.toString(template.getUnorderedListMarkerPattern(), DEFAULT_UNORDERED_LIST_MARKER_REGEX);
    }

    TemplateCompilerConfig() {
        this.unorderedListMarkerPattern = DEFAULT_UNORDERED_LIST_MARKER_REGEX;
    }

    String getUnorderedListMarkerPattern() {
        return unorderedListMarkerPattern;
    }
}
