package io.slgl.api.it.utils;

import java.util.List;
import java.util.Map;

public class MapUtils {

    private MapUtils() {
    }

    public static Object get(Map<?, ?> map, Object... path) {
        Object value = map;

        for (Object pathElement : path) {
            if (value == null) {
                return null;
            }

            if (value instanceof Map) {
                value = ((Map<?, ?>) value).get(pathElement);

            } else if (value instanceof List && pathElement instanceof Number) {
                value = ((List<?>) value).get(((Number)pathElement).intValue());

            } else {
                return null;
            }
        }

        return value;
    }
}
