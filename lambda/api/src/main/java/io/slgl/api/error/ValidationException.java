package io.slgl.api.error;

import io.slgl.api.utils.PathPrefix;
import lombok.Getter;

import javax.validation.ConstraintViolation;
import java.util.Set;

@Getter
public class ValidationException extends RuntimeException {

    private Set<ConstraintViolation<Object>> constraintViolations;
    private PathPrefix pathPrefix;

    public ValidationException(Set<ConstraintViolation<Object>> constraintViolations, PathPrefix pathPrefix) {
        this.constraintViolations = constraintViolations;
        this.pathPrefix = pathPrefix;
    }
}
