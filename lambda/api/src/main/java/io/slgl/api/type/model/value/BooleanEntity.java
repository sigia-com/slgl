package io.slgl.api.type.model.value;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

@Data
@Accessors(chain = true)
public class BooleanEntity {

    @NotNull
    private Boolean value;
}
