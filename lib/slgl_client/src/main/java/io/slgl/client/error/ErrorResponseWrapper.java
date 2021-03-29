package io.slgl.client.error;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorResponseWrapper {

	private final ErrorResponse error;

	@JsonCreator
	public ErrorResponseWrapper(@JsonProperty("error") ErrorResponse error) {
		this.error = error;
	}

	public ErrorResponse getError() {
		return error;
	}
}
