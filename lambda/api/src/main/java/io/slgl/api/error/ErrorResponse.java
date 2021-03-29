package io.slgl.api.error;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
public class ErrorResponse {

	private Error error;

	@JsonCreator
	protected ErrorResponse() {
	}

	public ErrorResponse(String code, String message, List<FieldError> fields) {
		this.error = new Error()
				.setCode(code)
				.setMessage(message)
				.setFields(fields);
	}

	@Data
	@Accessors(chain = true)
	public static class Error {

		private String code;
		private String message;

		private List<FieldError> fields;
	}

	@Data
	@Accessors(chain = true)
	public static class FieldError {

		private String field;
		private String code;
		private String message;
	}
}
