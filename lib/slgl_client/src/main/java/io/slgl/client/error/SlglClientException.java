package io.slgl.client.error;

public class SlglClientException extends RuntimeException {

    public SlglClientException(String message) {
        super(message);
    }

    public SlglClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public SlglClientException(Throwable cause) {
        super(cause);
    }
}
