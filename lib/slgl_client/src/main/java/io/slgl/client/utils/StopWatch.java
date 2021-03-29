package io.slgl.client.utils;

import java.time.LocalDateTime;
import java.time.temporal.TemporalUnit;

public class StopWatch {

    private LocalDateTime started = LocalDateTime.now();

    public static StopWatch start() {
        return new StopWatch();
    }

    public long getTime(TemporalUnit unit) {
        return started.until(LocalDateTime.now(), unit);
    }
}
