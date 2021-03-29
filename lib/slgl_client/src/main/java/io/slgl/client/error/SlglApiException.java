package io.slgl.client.error;

public class SlglApiException extends SlglClientException {

	private final ErrorResponse errorResponse;
	private final int httpStatus;

    public SlglApiException(ErrorResponse errorResponse, int httpStatus) {
        super(buildMessage(errorResponse, httpStatus));

        this.errorResponse = errorResponse;
        this.httpStatus = httpStatus;
    }

    private static String buildMessage(ErrorResponse errorResponse, int httpStatus) {
        return httpStatus + ": " + errorResponse.detailedMessage();
    }

    public ErrorResponse getErrorResponse() {
        return this.errorResponse;
    }

    public Integer getHttpStatus() {
        return this.httpStatus;
    }
}
