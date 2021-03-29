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
@Pattern(pattern = "(http|https)://([^\\s/?.#]+\\.?)+(/[^\\s#]*)?", message = "must be well-formed URL")
@Length(max = 250)
@Documented
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
public @interface ValidUrl {

    String message() default "must be valid URL";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
