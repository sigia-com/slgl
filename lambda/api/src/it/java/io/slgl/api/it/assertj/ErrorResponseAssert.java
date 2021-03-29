package io.slgl.api.it.assertj;

import io.slgl.api.utils.ErrorCode;
import io.slgl.client.error.SlglApiException;
import org.assertj.core.api.AbstractThrowableAssert;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("UnusedReturnValue")
public class ErrorResponseAssert extends AbstractThrowableAssert<ErrorResponseAssert, SlglApiException> {
    public ErrorResponseAssert(SlglApiException e) {
        super(e, ErrorResponseAssert.class);
    }

    public ErrorResponseAssert hasErrorCode(ErrorCode errorCode, String... params) {
        hasHttpStatus(errorCode.getHttpStatus());
        hasErrorCode(errorCode.getCode());
        hasErrorMessage(errorCode, params);
        return this;
    }

    public ErrorResponseAssert hasErrorMessage(ErrorCode errorCode, Object[] params) {
        assertThat(actual.getErrorResponse().getMessage()).isEqualTo(errorCode.getMessage(params));
        return this;
    }

    public ErrorResponseAssert errorMessageContains(String message) {
        assertThat(actual.getErrorResponse().getMessage()).contains(message);
        return this;
    }

    public ErrorResponseAssert hasErrorCode(String code) {
        assertThat(actual.getErrorResponse().getCode()).isEqualTo(code);
        return this;
    }

    public ErrorResponseAssert hasHttpStatus(int httpStatus) {
        assertThat(actual.getHttpStatus()).isEqualTo(httpStatus);
        return this;
    }


    public ErrorResponseAssert hasValidationError(Pattern fieldNameRegex, String validationCode) {
        hasErrorCode(ErrorCode.VALIDATION_ERROR);
        assertThat(actual.getErrorResponse().getFields()).anySatisfy(it -> {
            assertThat(it.getField()).matches(fieldNameRegex);
            if (validationCode != null) {
                assertThat(it.getCode()).isEqualTo(validationCode);
            }
        });
        return this;
    }
}
