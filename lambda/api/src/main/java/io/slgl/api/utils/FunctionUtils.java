package io.slgl.api.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
public abstract class FunctionUtils {
    private FunctionUtils() {
    }


    public static <T> void forEachEnsureEachCalled(Iterable<T> iterable, Consumer<? super T> consumer) {
        Throwable exception = null;
        for (T t : iterable) {
            try {
                consumer.accept(t);
            } catch (Throwable e) {
                log.error("Failed consumer {} ", consumer, e);
                exception = e;
            }
        }
        if (exception != null) {
            throw new RuntimeException("#forEachEnsureEachCalled failed, last exception was:", exception);
        }
    }
}
