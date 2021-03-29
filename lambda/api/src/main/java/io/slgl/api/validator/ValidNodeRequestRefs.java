package io.slgl.api.validator;


import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Constraint(validatedBy = {ValidNodeRequestRefsValidator.class})
@Documented
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
public @interface ValidNodeRequestRefs {

    String message() default "{io.slgl.validator.ValidNodeRequestRefs.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
