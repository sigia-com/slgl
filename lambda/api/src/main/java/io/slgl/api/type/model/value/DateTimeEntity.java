package io.slgl.api.type.model.value;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.time.Instant;

@Data
@Accessors(chain = true)
public class DateTimeEntity {

    @NotNull
    private Instant value;
}
