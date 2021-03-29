package io.slgl.client.error;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ErrorResponse {

    private final String code;
    private final String message;
    private final List<FieldError> fields;

    @JsonCreator
    public ErrorResponse(
            @JsonProperty("code") String code,
            @JsonProperty("message") String message,
            @JsonProperty("fields") List<FieldError> fields) {
        this.code = code;
        this.message = message;
        this.fields = fields;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public List<FieldError> getFields() {
        return fields;
    }

    public String detailedMessage() {
        if (fields == null) {
            return message + " (" + code + ")";
        } else {
            return message + " (" + code + ") : " + fields;
        }
    }

    public static class FieldError {
        private final String field;
        private final String code;
        private final String message;

        @JsonCreator
        public FieldError(
                @JsonProperty("field") String field,
                @JsonProperty("code") String code,
                @JsonProperty("message") String message) {
            this.field = field;
            this.code = code;
            this.message = message;
        }

        @Override
        public String toString() {
            return field + "([" + code + "]" + message + ')';
        }

        public String getField() {
            return this.field;
        }

        public String getCode() {
            return this.code;
        }

        public String getMessage() {
            return this.message;
        }
    }
}
