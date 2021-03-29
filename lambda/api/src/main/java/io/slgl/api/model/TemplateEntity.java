package io.slgl.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.slgl.api.validator.ValidFreemarkerTemplate;
import io.slgl.template.Template;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@EqualsAndHashCode
@JsonIgnoreProperties(value = {"@type"})
public class TemplateEntity {

    @ValidFreemarkerTemplate
    private String text;

    private String unorderedListMarkerPattern;

    @JsonIgnore
    public Template toClientDTO() {
        return new Template(getText(), getUnorderedListMarkerPattern());
    }
}
