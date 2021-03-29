package io.slgl.api.validator;

import org.hibernate.validator.constraints.Length;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Constraint(validatedBy = {})
@Pattern(pattern = "\\#\\S+", message = "must start with # character and contain only non-whitespace characters")
@Length(max = 100)
@Documented
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
public @interface ValidAnchor {

    String message() default "must be valid SLGL Anchor";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
