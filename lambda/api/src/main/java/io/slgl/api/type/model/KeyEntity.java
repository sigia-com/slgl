package io.slgl.api.type.model;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;

@Data
@Accessors(chain = true)
public class KeyEntity {

    @NotEmpty
    @Length(max = 100)
    private String value;
}
