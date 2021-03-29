package io.slgl.api.it.properties;

import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
class CompoundPropertyLoader implements PropertyLoader {
    private final List<PropertyLoader> propertyLoaders;

    @Override
    public String getString(String property) {
        for (PropertyLoader propertyLoader : propertyLoaders) {
            var result = propertyLoader.getString(property);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
