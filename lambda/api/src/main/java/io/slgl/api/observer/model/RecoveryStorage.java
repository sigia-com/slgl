package io.slgl.api.observer.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
public class RecoveryStorage {

    @NotEmpty
    String path;
}
