package io.slgl.client.node.permission;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableList;

public class RequirementsList implements Requirements {

    @JsonValue
    private final List<SimpleRequirements> requirements;

    @JsonCreator
    public RequirementsList(List<SimpleRequirements> requirements) {
        this.requirements = unmodifiableList(requirements);
    }

    @Override
    public Stream<Requirement> stream() {
        return requirements.stream().flatMap(SimpleRequirements::stream);
    }

    public List<SimpleRequirements> getRequirements() {
        return requirements;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequirementsList that = (RequirementsList) o;
        return Objects.equals(requirements, that.requirements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requirements);
    }
}
