package io.slgl.api.type;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.slgl.api.model.TypeEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class BuiltinTypeEntity extends TypeEntity {

    @JsonProperty("@id")
    private String id;
}
