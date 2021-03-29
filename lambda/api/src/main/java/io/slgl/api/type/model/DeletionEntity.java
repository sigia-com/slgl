package io.slgl.api.type.model;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotEmpty;

@Data
@Accessors(chain = true)
public class DeletionEntity {

    @NotEmpty
    private String reason;
}
