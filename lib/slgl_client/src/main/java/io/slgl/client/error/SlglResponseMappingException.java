package io.slgl.client.error;

public class SlglResponseMappingException extends SlglClientException {

    private final int httpStatus;

    public SlglResponseMappingException(String message, int httpStatus) {
        super(buildMessage(message, httpStatus));
        this.httpStatus = httpStatus;
    }

    public SlglResponseMappingException(String message, int httpStatus, Throwable cause) {
        super(buildMessage(message, httpStatus), cause);
        this.httpStatus = httpStatus;
    }

    public SlglResponseMappingException(Throwable cause, int httpStatus) {
        super(cause);
        this.httpStatus = httpStatus;
    }

    private static String buildMessage(String message, int httpStatus) {
        return httpStatus + ": " + message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
