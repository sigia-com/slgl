package io.slgl.api.validator;

import io.slgl.api.ExecutionContext;
import io.slgl.api.service.CurrentUserService;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import static com.google.common.base.Objects.equal;

public class MustBeCurrentUserValidator implements ConstraintValidator<MustBeCurrentUser, String> {

    private final CurrentUserService currentUserService = ExecutionContext.get(CurrentUserService.class);

    private MustBeCurrentUser constraint;

    @Override
    public void initialize(MustBeCurrentUser constraint) {
        this.constraint = constraint;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return equal(value, currentUserService.getCurrentUser().getId());
    }
}
