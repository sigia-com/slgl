package io.slgl.streamprocessor.utils;

import java.util.HashMap;
import java.util.Map;

public class LambdaEnv {

    private static final Map<String, String> values = new HashMap<>();

    private LambdaEnv() {
    }

    public static String getEntriesSnsTopic() {
        return get("SLGL_SNS_TOPIC");
    }

    private static String get(String key) {
        if (!values.containsKey(key)) {
            values.put(key, System.getenv(key));
        }

        return values.get(key);
    }

    public static void override(Map<String, String> values) {
        if (values == null) {
            return;
        }

        LambdaEnv.values.putAll(values);
    }
}
