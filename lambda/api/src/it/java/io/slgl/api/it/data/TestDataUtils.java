package io.slgl.api.it.data;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.UUID;

public class TestDataUtils {

    private static final String UNIQUE_BASE = System.currentTimeMillis() + "-" + RandomStringUtils.randomAlphanumeric(6);
    private static int counter = 1;

    private TestDataUtils() {
    }

    public static String generateUniqueId() {
        return UUID.randomUUID().toString().toLowerCase();
    }

    public static synchronized String generateUniqueStateSourceId() {
        return UNIQUE_BASE + "/" + (counter++);
    }
}
