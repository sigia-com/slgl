package io.slgl.api.it.properties;

import lombok.Cleanup;
import lombok.SneakyThrows;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

class PropertiesFilePropertyLoader implements PropertyLoader {
    private final Properties properties;

    @SneakyThrows
    public PropertiesFilePropertyLoader(String defaultFile, String systemKey) {
        this.properties = new Properties();
        var propertyFileName = System.getProperty(systemKey, defaultFile);
        load(propertyFileName);
    }

    private void load(String propertyFileName) throws IOException {
        var propertyFile = new File(propertyFileName);
        if (propertyFile.exists()) {
            @Cleanup var propertyFileReader = new FileReader(propertyFile);
            properties.load(propertyFileReader);
        }
    }

    @Override
    public String getString(String property) {
        return properties.getProperty(property);
    }
}
