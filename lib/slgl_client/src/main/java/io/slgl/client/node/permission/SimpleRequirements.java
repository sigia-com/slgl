package io.slgl.client.node.permission;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableMap;

public class SimpleRequirements implements Requirements {

    @JsonValue
    private final Map<String, Requirement.Spec> requirements;

    @JsonCreator
    public SimpleRequirements(Map<String, Requirement.Spec> requirements) {
        this.requirements = unmodifiableMap(requirements);
    }

    @Override
    public Stream<Requirement> stream() {
        return requirements.entrySet()
                .stream()
                .map(it -> new Requirement(it.getKey(), it.getValue()));
    }

    public Map<String, Requirement.Spec> asMap() {
        return requirements;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleRequirements that = (SimpleRequirements) o;
        return Objects.equals(requirements, that.requirements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requirements);
    }
}
