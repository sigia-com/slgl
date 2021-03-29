package io.slgl.api.utils.json;

public class FieldPathBuilder {

    private final StringBuilder path = new StringBuilder();

    public String build() {
        if (path.length() == 0) {
            return "<root>";
        }

        return path.toString();
    }

    public void appendField(String fieldName) {
        if (fieldName == null) {
            path.append("<null>");

        } else if (fieldName.isEmpty()) {
            path.append("<empty>");

        } else if (fieldName.contains(".")) {
            var escaped = fieldName.replaceAll("'", "\\'");
            path.append("['").append(escaped).append("']");

        } else {
            if (path.length() > 0) {
                path.append('.');
            }
            path.append(fieldName);
        }
    }

    public void appendIndex(int index) {
        path.append("[").append(index).append("]");
    }

    public void append(Object object) {
        if (object instanceof Number) {
            appendIndex(((Number) object).intValue());
        } else {
            appendField(String.valueOf(object));
        }
    }
}
