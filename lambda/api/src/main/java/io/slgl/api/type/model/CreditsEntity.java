package io.slgl.api.type.model;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@Accessors(chain = true)
public class CreditsEntity {

    @NotNull
    @Min(value = 1)
    private Long creditsAmount;
}
