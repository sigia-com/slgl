package io.slgl.api.validator;

import io.slgl.api.ExecutionContext;
import io.slgl.api.error.ApiException;
import io.slgl.api.model.TypeEntity;
import io.slgl.api.permission.service.PermissionsMerger;
import io.slgl.api.type.Anchor;
import io.slgl.api.type.TypeFactory;
import io.slgl.api.type.TypeMatcher;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

public class ValidTypeValidator implements ConstraintValidator<ValidType, TypeEntity> {

    private final TypeFactory typeFactory = ExecutionContext.get(TypeFactory.class);
    private final PermissionsMerger permissionsMerger = ExecutionContext.get(PermissionsMerger.class);
    private final TypeMatcher typeMatcher = ExecutionContext.get(TypeMatcher.class);

    private ValidType constraint;

    @Override
    public void initialize(ValidType constraint) {
        this.constraint = constraint;
    }

    @Override
    public boolean isValid(TypeEntity entity, ConstraintValidatorContext context) {
        if (entity == null) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        boolean isValid = true;

        var type = typeFactory.create(entity);

        if (entity.getPermissions() != null) {
            try {
                permissionsMerger.validatePermissions(entity.getPermissions(), type.getPermissions());
            } catch (ApiException e) {
                isValid = false;
                context.buildConstraintViolationWithTemplate(e.getMessage())
                        .addPropertyNode("permissions")
                        .addConstraintViolation();
            }
        }

        if (entity.getAnchors() != null) {
            Map<String, Anchor> parentTypeAnchors = Collections.emptyMap();
            if (type.getParentType() != null) {
                parentTypeAnchors = type.getParentType().getAnchors().stream()
                        .collect(Collectors.toMap(Anchor::getId, identity()));
            }

            int anchorIndex = 0;
            for (TypeEntity.AnchorEntity anchorEntity : entity.getAnchors()) {

                Anchor overriddenAnchor = parentTypeAnchors.get(anchorEntity.getId());
                if (overriddenAnchor != null) {
                    if (!typeMatcher.isEqualOrExtendingType(
                            overriddenAnchor.getType().orElse(null),
                            new Anchor(anchorEntity).getType().orElse(null))) {

                        isValid = false;
                        context.buildConstraintViolationWithTemplate("type of overridden anchor '" + anchorEntity.getId() + "' must extends anchor type that is defined in parent type")
                                .addContainerElementNode("anchors", List.class, anchorIndex)
                                .addPropertyNode("type")
                                .addConstraintViolation();
                    }
                }

                anchorIndex++;
            }
        }

        return isValid;
    }
}
