package io.slgl.client.audit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class EvaluationLogEntry {
    private final String code;
    private final CharSequence details;
    @JsonCreator
    public EvaluationLogEntry(
            @JsonProperty("code") String code,
            @JsonProperty("details") CharSequence details
    ) {
        this.code = code;
        this.details = details;
    }

    @Override
    public String toString() {
        return "EvaluationLogEntry{" +
                "code=" + code +
                ", details='" + details + '\'' +
                '}';
    }

    @JsonProperty("code")
    public String getCode() {
        return code;
    }

    @JsonProperty("details")
    public String getMessage() {
        return details == null ? null : details.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvaluationLogEntry that = (EvaluationLogEntry) o;
        return Objects.equals(code, that.code) &&
                Objects.equals(details, that.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, details);
    }
}
