package io.slgl.api.validator;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.slgl.api.error.ValidationException;
import lombok.Data;
import lombok.experimental.Accessors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.constraints.NotNull;

import static org.assertj.core.api.Assertions.assertThat;

class ValidatorServiceTest {

    private final ValidatorService validatorService = new ValidatorService();

    @Test
    public void shouldUseJsonNamingStrategyForPathsInValidationResults() {
        // given
        ValidationTest testObject = new ValidationTest();

        // when
        ValidationException exception = expectException(() -> validatorService.validate(testObject));

        // then
        assertThat(exception.getConstraintViolations()).hasSize(1);
        ConstraintViolation<?> constraintViolation = exception.getConstraintViolations().iterator().next();

        assertThat(constraintViolation.getPropertyPath().iterator().next().getName()).isEqualTo("test_field");
    }

    @Test
    public void shouldUseJsonAnnotationsForPathsInValidationResults() {
        // given
        ValidationTestWithJsonAnnotation testObject = new ValidationTestWithJsonAnnotation();

        // when
        ValidationException exception = expectException(() -> validatorService.validate(testObject));

        // then
        assertThat(exception.getConstraintViolations()).hasSize(1);
        ConstraintViolation<?> constraintViolation = exception.getConstraintViolations().iterator().next();

        assertThat(constraintViolation.getPropertyPath().iterator().next().getName()).isEqualTo("@test_field");
    }

    private ValidationException expectException(Runnable call) {
        try {
            call.run();

            return Assertions.fail("Expected validation error");
        } catch (ValidationException e) {
            return e;
        }
    }

    @Data
    @Accessors(chain = true)
    public static class ValidationTest {

        @NotNull
        public String testField;
    }

    @Data
    @Accessors(chain = true)
    public static class ValidationTestWithJsonAnnotation {

        @JsonProperty("@test_field")
        @NotNull
        public String testField;
    }
}
