package io.slgl.template;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class Template {

    private final String text;
    private final String unorderedListMarkerPattern;

    public Template(String text) {
        this(text, null);
    }

    @JsonCreator
    public Template(
            @JsonProperty("text") String text,
            @JsonProperty("unordered_list_marker_pattern") String unorderedListMarkerPattern
    ) {
        this.text = text;
        this.unorderedListMarkerPattern = unorderedListMarkerPattern;
    }

    @JsonProperty("text")
    public String getText() {
        return text;
    }

    @JsonProperty("unordered_list_marker_pattern")
    public String getUnorderedListMarkerPattern() {
        return unorderedListMarkerPattern;
    }
}
