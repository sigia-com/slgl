package io.slgl.api.it.properties;

interface PropertyLoader {

    String getString(String property);

    default String getString(String property, String defaultValue) {
        var result = getString(property);
        return result != null ? result : defaultValue;
    }

}
