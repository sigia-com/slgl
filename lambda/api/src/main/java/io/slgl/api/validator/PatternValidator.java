package io.slgl.api.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class PatternValidator implements ConstraintValidator<Pattern, String> {

    private Pattern constraint;
    private java.util.regex.Pattern regexPattern;

    @Override
    public void initialize(Pattern constraint) {
        this.constraint = constraint;
        regexPattern = java.util.regex.Pattern.compile(constraint.pattern());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return regexPattern.matcher(value).matches();
    }
}
