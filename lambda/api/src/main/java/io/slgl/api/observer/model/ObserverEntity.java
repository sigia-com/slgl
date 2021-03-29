package io.slgl.api.observer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import static java.util.Objects.nonNull;

@Accessors(chain = true)
@Getter
@Setter
public class ObserverEntity {

    @NotEmpty
    private String pgpKey;

    @Valid
    private RecoveryStorage recoveryStorage;

    @JsonProperty("aws_s3")
    @NotNull
    @Valid
    private S3Storage s3Storage;

    public boolean hasStorage() {
        return nonNull(s3Storage);
    }

    public Storage getStorage() {
        return s3Storage;
    }
}
