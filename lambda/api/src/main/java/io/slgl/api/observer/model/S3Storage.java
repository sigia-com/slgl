package io.slgl.api.observer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

@Accessors(chain = true)
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class S3Storage implements Storage {

    @NotEmpty
    private String region;

    @NotEmpty
    private String bucket;

    @NotEmpty
    private String path;

    @Valid
    private S3Credentials credentials;
}
