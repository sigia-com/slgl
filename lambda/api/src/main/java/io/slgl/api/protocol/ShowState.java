package io.slgl.api.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ShowState {
    DO_NOT_SHOW("do_not_show", false, false),
    SHOW_FAIL_ON_UNAUTHORIZED("show_fail_on_unauthorized", true, true),
    SHOW_DO_NOT_FAIL_ON_UNAUTHORIZED("show_do_not_fail_on_unauthorized", true, false);

    private String value;
    private boolean appendState;
    private boolean failOnNotAuthorized;

    @JsonCreator
    public static ShowState forValue(String value) {
        for (ShowState val : values()) {
            if (val.getValue().equalsIgnoreCase(value)) {
                return val;
            }
        }
        throw new IllegalArgumentException("Unrecognized showState value: " + value);
    }

    @JsonValue
    public String toValue() {
        return value;
    }
}
