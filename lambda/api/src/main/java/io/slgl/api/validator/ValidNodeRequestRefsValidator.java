package io.slgl.api.validator;

import io.slgl.api.protocol.ApiRequestItem;
import io.slgl.api.protocol.LinkRequest;
import io.slgl.api.protocol.NodeRequest;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

public class ValidNodeRequestRefsValidator implements ConstraintValidator<ValidNodeRequestRefs, List<ApiRequestItem>> {

    @Override
    public boolean isValid(List<ApiRequestItem> items, ConstraintValidatorContext context) {
        if (items == null) {
            return true;
        }

        boolean valid = true;

        for (int i = 0; i < items.size(); i++) {
            var item = items.get(i);

            if (item instanceof LinkRequest) {
                var link = (LinkRequest) item;

                if (!validateNodeReferenceInLink(link.getSourceNodeRef(), items, i, context, "source_node")) {
                    valid = false;
                }
                if (!validateNodeReferenceInLink(link.getTargetNodeRef(), items, i, context, "target_node")) {
                    valid = false;
                }
            }
        }
        return valid;
    }

    private boolean validateNodeReferenceInLink(Integer reference, List<ApiRequestItem> items, int indexOfLink, ConstraintValidatorContext context, String refPropertyName) {
        if (reference == null) {
            return true;
        }
        if (reference >= indexOfLink) {
            addViolation(context, "cannot be bigger than index of referring link", refPropertyName, indexOfLink);
            return false;
        }
        var ref = items.get(reference);
        if (!(ref instanceof NodeRequest)) {
            addViolation(context, "can only refer to request of type `node`", refPropertyName, indexOfLink);
            return false;
        }
        return true;
    }

    private void addViolation(ConstraintValidatorContext context, String message, String propertyName, int index) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(propertyName).inIterable().atIndex(index)
                .addConstraintViolation();
    }
}
