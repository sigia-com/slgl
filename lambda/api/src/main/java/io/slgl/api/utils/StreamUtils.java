package io.slgl.api.utils;

import java.util.function.Function;
import java.util.stream.Stream;

public abstract class StreamUtils {
    private StreamUtils() {
    }


    public static <T> Function<Object, Stream<T>> filterInstance(Class<T> clazz) {
        return o -> clazz.isInstance(o)
                ? Stream.of((clazz.cast(o)))
                : Stream.empty();
    }

}
