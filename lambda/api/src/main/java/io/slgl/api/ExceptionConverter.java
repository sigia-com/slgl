package io.slgl.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.PropertyBindingException;
import io.slgl.api.error.ApiException;
import io.slgl.api.error.ErrorResponse;
import io.slgl.api.error.UnrecognizedFieldException;
import io.slgl.api.error.ValidationException;
import io.slgl.api.utils.ErrorCode;
import io.slgl.api.utils.PathPrefix;
import io.slgl.api.utils.json.FieldPathBuilder;
import io.slgl.api.utils.json.UncheckedObjectMapper;
import io.slgl.api.utils.json.UncheckedObjectMapper.JsonException;
import io.slgl.api.utils.json.UncheckedObjectMapper.NestedJsonException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.qldbsession.model.OccConflictException;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Objects.equal;

@Slf4j
public class ExceptionConverter {

    public ApiException convertToApiException(Throwable e) {

        if (e instanceof javax.validation.ValidationException) {
            e = e.getCause();
        }

        if (e instanceof ApiException) {
            return (ApiException) e;
        }

        if (e instanceof JsonProcessingException) {
            return convertJsonException((JsonProcessingException) e);
        }
        if (e instanceof JsonException && e.getCause() instanceof JsonProcessingException) {
            return convertJsonException((JsonProcessingException) e.getCause());
        }

        if (e instanceof ValidationException) {
            return convertValidationException((ValidationException) e);
        }

        if (e instanceof OccConflictException) {
            return new ApiException(ErrorCode.CONCURRENCY_CONFLICT);
        }

        return new ApiException(ErrorCode.UNKNOWN_ERROR);
    }

    private ApiException convertJsonException(JsonProcessingException e) {
        if (e instanceof JsonMappingException) {
            JsonProcessingException rootException = getRootJsonException(e);

            if (rootException instanceof PropertyBindingException || rootException.getCause() instanceof UnrecognizedFieldException) {
                return new ApiException(ErrorCode.UNABLE_TO_PARSE_UNRECOGNIZED_FIELD, getFieldPath((JsonMappingException) e));
            } else {
                return new ApiException(ErrorCode.UNABLE_TO_PARSE_FIELD, getFieldPath((JsonMappingException) e));
            }
        }

        return new ApiException(ErrorCode.UNABLE_TO_PARSE_REQUEST);
    }

    private JsonMappingException getRootJsonException(Throwable e) {
        Throwable cause = getCauseOrNestedException(e);
        if (cause != null) {
            JsonMappingException rootException = getRootJsonException(cause);
            if (rootException != null) {
                return rootException;
            }
        }

        if (e instanceof JsonMappingException) {
            return (JsonMappingException) e;
        }

        return null;
    }

    private String getFieldPath(JsonMappingException e) {
        var pathBuilder = new FieldPathBuilder();

        buildFieldPath(pathBuilder, e);

        return pathBuilder.build();
    }

    private void buildFieldPath(FieldPathBuilder pathBuilder, Throwable e) {
        if (e instanceof JsonMappingException) {
            for (JsonMappingException.Reference pathSegment : ((JsonMappingException)e).getPath()) {
                if (pathSegment.getFieldName() != null) {
                    pathBuilder.appendField(pathSegment.getFieldName());
                }
                if (pathSegment.getIndex() != -1) {
                    pathBuilder.appendIndex(pathSegment.getIndex());
                }
            }
        }

        Throwable cause = getCauseOrNestedException(e);
        if (cause != null) {
            buildFieldPath(pathBuilder, cause);
        }
    }

    private static Throwable getCauseOrNestedException(Throwable e) {
        if (e instanceof NestedJsonException) {
            return ((NestedJsonException) e).getNestedException();
        } else {
            return e.getCause();
        }
    }

    private ApiException convertValidationException(ValidationException e) {
        List<ErrorResponse.FieldError> fieldErrors = new ArrayList<>();

        if (e.getConstraintViolations() != null) {
            for (ConstraintViolation<?> constraintViolation : e.getConstraintViolations()) {
                fieldErrors.add(new ErrorResponse.FieldError()
                        .setField(getFieldPath(e.getPathPrefix(), constraintViolation))
                        .setCode(getCode(constraintViolation))
                        .setMessage(constraintViolation.getMessage()));
            }
        }

        return new ApiException(fieldErrors);
    }

    private String getFieldPath(PathPrefix pathPrefix, ConstraintViolation<?> constraintViolation) {
        var pathBuilder = new FieldPathBuilder();

        for (Object pathSegment : pathPrefix.getPathSegments()) {
            pathBuilder.append(pathSegment);
        }

        for (Path.Node node : constraintViolation.getPropertyPath()) {
            if (node.getIndex() != null) {
                pathBuilder.appendIndex(node.getIndex());
            }

            if (node.getKey() != null) {
                pathBuilder.append(node.getKey());
            }

            if (node.getName() != null && !node.getName().isEmpty() && !equal(node.getName(), "<map value>")) {
                pathBuilder.appendField(node.getName());
            }
        }

        return pathBuilder.build();
    }

    private String getCode(ConstraintViolation<?> constraintViolation) {
        String annotationFullName = constraintViolation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();

        int dotIndex = StringUtils.lastIndexOf(annotationFullName, ".");
        String annotationName = StringUtils.substring(annotationFullName, dotIndex + 1);

        return convertName(annotationName);
    }

    private String convertName(String name) {
        return UncheckedObjectMapper.MAPPER.getPropertyNamingStrategy().nameForField(null, null, name);
    }
}
