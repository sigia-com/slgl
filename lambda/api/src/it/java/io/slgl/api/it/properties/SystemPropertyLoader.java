package io.slgl.api.it.properties;

public class SystemPropertyLoader implements PropertyLoader {
    @Override
    public String getString(String property) {
        return System.getProperty(property);
    }
}
