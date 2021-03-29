package io.slgl.api.type.model.value;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class NumberEntity {

    @NotNull
    private BigDecimal value;
}
