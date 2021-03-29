package io.slgl.api.it.assertj;

import io.slgl.api.utils.ErrorCode;
import io.slgl.client.error.ErrorResponse;
import org.assertj.core.api.AbstractAssert;

import static org.assertj.core.api.Assertions.assertThat;

public class ErrorAssert extends AbstractAssert<ErrorAssert, ErrorResponse> {

	public ErrorAssert(ErrorResponse actual) {
		super(actual, ErrorAssert.class);
	}

	public ErrorAssert hasErrorCode(ErrorCode errorCode) {
		assertThat(actual.getCode()).isEqualTo(errorCode.getCode());
		return myself;
	}

	public ErrorAssert hasErrorCodeAndMessage(ErrorCode errorCode, Object... params) {
		assertThat(actual.getCode()).isEqualTo(errorCode.getCode());
		assertThat(actual.getMessage()).isEqualTo(errorCode.getMessage(params));
		return myself;
	}

	public ErrorAssert hasFieldError(String field, String code) {
		assertThat(actual.getFields()).isNotNull();

		assertThat(actual.getFields())
				.extracting(ErrorResponse.FieldError::getField)
				.contains(field);

		assertThat(actual.getFields())
				.filteredOn(it -> it.getField().equals(field))
				.extracting(ErrorResponse.FieldError::getCode)
				.contains(code);

		return myself;
	}

	public ErrorAssert hasOnlyFieldError(String field, String code) {
		hasFieldError(field, code);
		assertThat(actual.getFields()).hasSize(1);
		return this;
	}

    public ErrorAssert messageContains(CharSequence value) {
		assertThat(actual.getMessage()).contains(value);
		return this;
    }

    public ErrorAssert fieldErrorContainsMessage(String field, CharSequence value) {
		String fieldMessage = actual.getFields().stream()
				.filter(f -> f.getField().equals(field))
				.map(ErrorResponse.FieldError::getMessage)
				.findFirst()
				.orElse(null);

		assertThat(fieldMessage).contains(value);

		return this;
    }
}
