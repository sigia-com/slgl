package io.slgl.api.utils;

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

@Slf4j
public class TimerUtils {

    public static <T> T getWithTimer(String name, Supplier<T> call) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            log.info("Starting timer: name={}", name);
            return call.get();
        } finally {
            log.info("Timer: name={}, time={}", name, stopwatch);
        }
    }

    public static void runWithTimer(String name, Runnable call) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            log.info("Starting timer: name={}", name);
            call.run();
        } finally {
            log.info("Timer: name={}, time={}", name, stopwatch);
        }
    }
}
