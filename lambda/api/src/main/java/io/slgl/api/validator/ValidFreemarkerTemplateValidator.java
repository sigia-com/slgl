package io.slgl.api.validator;

import io.slgl.api.ExecutionContext;
import io.slgl.template.FreemarkerTemplateRenderer;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ValidFreemarkerTemplateValidator implements ConstraintValidator<ValidFreemarkerTemplate, String> {

    private final FreemarkerTemplateRenderer freemarkerTemplateRenderer = ExecutionContext.get(FreemarkerTemplateRenderer.class);

    private ValidFreemarkerTemplate constraint;

    @Override
    public void initialize(ValidFreemarkerTemplate constraint) {
        this.constraint = constraint;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        try {
            freemarkerTemplateRenderer.validate(value);

            return true;

        } catch (Exception e) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(constraint.message() + ": " + e.getMessage())
                    .addConstraintViolation();

            return false;
        }
    }
}
