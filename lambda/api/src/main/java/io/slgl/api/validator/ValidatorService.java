package io.slgl.api.validator;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import io.slgl.api.error.ValidationException;
import io.slgl.api.utils.PathPrefix;
import io.slgl.api.utils.json.UncheckedObjectMapper;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.spi.nodenameprovider.JavaBeanProperty;
import org.hibernate.validator.spi.nodenameprovider.Property;
import org.hibernate.validator.spi.nodenameprovider.PropertyNodeNameProvider;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.lang.reflect.Field;
import java.util.Set;

public class ValidatorService {

    private final Validator validator;

    public ValidatorService() {
        ValidatorFactory factory = Validation.byProvider(HibernateValidator.class).configure()
                .propertyNodeNameProvider(new JacksonAwarePropertyNodeNameProvider())
                .buildValidatorFactory();

        validator = factory.getValidator();
    }

    public void validate(Object object) {
        validate(object, PathPrefix.empty());
    }

    public void validate(Object object, PathPrefix pathPrefix) {
        Set<ConstraintViolation<Object>> constraintViolations = validator.validate(object);

        if (!constraintViolations.isEmpty()) {
            throw new ValidationException(constraintViolations, pathPrefix);
        }
    }

    public static class JacksonAwarePropertyNodeNameProvider implements PropertyNodeNameProvider {

        private final ObjectMapper objectMapper = UncheckedObjectMapper.MAPPER;

        @Override
        public String getName(Property property) {
            if (property instanceof JavaBeanProperty) {
                JavaBeanProperty javaBeanProperty = (JavaBeanProperty) property;

                try {
                    Field field = javaBeanProperty.getDeclaringClass().getDeclaredField(javaBeanProperty.getName());
                    ValidationProperty validationProperty = field.getAnnotation(ValidationProperty.class);
                    if (validationProperty != null) {
                        return validationProperty.value();
                    }
                } catch (NoSuchFieldException ignore) {
                }

                JavaType type = objectMapper.constructType(javaBeanProperty.getDeclaringClass());
                BeanDescription desc = objectMapper.getSerializationConfig().introspect(type);

                return desc.findProperties()
                        .stream()
                        .filter(prop -> prop.getInternalName().equals(property.getName()))
                        .map(BeanPropertyDefinition::getName)
                        .findFirst()
                        .orElse(property.getName());
            }

            return property.getName();
        }
    }
}
