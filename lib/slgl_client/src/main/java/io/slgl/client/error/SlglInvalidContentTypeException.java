package io.slgl.client.error;

import org.apache.http.entity.ContentType;

public class SlglInvalidContentTypeException extends SlglResponseMappingException {

    private final ContentType contentType;
    private final String body;

    public SlglInvalidContentTypeException(ContentType contentType, String body, int httpStatus) {
        super(buildMessage(contentType, body), httpStatus);

        this.contentType = contentType;
        this.body = body;
    }

    private static String buildMessage(ContentType contentType, String body) {
        return String.format("contentType: %s, body: %s", contentType, body);
    }

    public ContentType getContentType() {
        return contentType;
    }

    public String getBody() {
        return body;
    }
}
