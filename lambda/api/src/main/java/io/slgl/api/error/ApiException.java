package io.slgl.api.error;

import io.slgl.api.utils.ErrorCode;
import lombok.Getter;

import java.util.List;

public class ApiException extends RuntimeException {

    @Getter
    private final ErrorCode errorCode;

    @Getter
    private final List<ErrorResponse.FieldError> fieldErrors;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, (Object[])null);
    }

    public ApiException(ErrorCode errorCode, Object... params) {
        super(errorCode.getMessage(params));

        this.errorCode = errorCode;
        this.fieldErrors = null;
    }

    public ApiException(List<ErrorResponse.FieldError> fieldErrors) {
        super(ErrorCode.VALIDATION_ERROR.getMessage());

        this.errorCode = ErrorCode.VALIDATION_ERROR;
        this.fieldErrors = fieldErrors;
    }

    public ErrorResponse createErrorResponse() {
        return new ErrorResponse(getErrorCode().getCode(), getMessage(), fieldErrors);
    }

    public String getCode() {
        return errorCode.getCode();
    }
}
