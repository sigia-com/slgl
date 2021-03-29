package io.slgl.api.utils;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;

import java.util.Arrays;
import java.util.stream.Collectors;

public class RepositoryUtils {

    private static final Escaper ESCAPER = Escapers.builder()
            .addEscape('|', "\\|")
            .addEscape('\\', "\\\\")
            .build();

    private RepositoryUtils() {
    }

    public static String buildIndexValue(String... values) {
        return Arrays.stream(values)
                .map(ESCAPER.asFunction())
                .collect(Collectors.joining("|"));
    }
}
