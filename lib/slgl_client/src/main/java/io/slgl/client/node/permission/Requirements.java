package io.slgl.client.node.permission;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singletonMap;

public interface Requirements extends Iterable<Requirement> {

    Stream<Requirement> stream();

    default List<Requirement> asList() {
        return stream().collect(Collectors.toList());
    }

    @Override
    default Iterator<Requirement> iterator() {
        return stream().iterator();
    }

    default List<Requirement> get(String path) {
        List<Requirement> result = new ArrayList<>();
        for (Requirement requirement : this) {
            if (path.equals(requirement.getPath())) {
                result.add(requirement);
            }
        }
        return result;
    }

    @JsonCreator
    static SimpleRequirements simple(Map<String, Requirement.Spec> requirement) {
        return new SimpleRequirements(requirement);
    }

    @JsonCreator
    static RequirementsList list(Collection<Map<String, Requirement.Spec>> requirements) {
        List<SimpleRequirements> list = requirements.stream()
                .map(SimpleRequirements::new)
                .collect(Collectors.toList());
        return new RequirementsList(list);
    }

    static Requirements of(Requirement... requirements) {
        return Requirements.builder().require(requirements).build();
    }

    static Requirements of(Iterable<Requirement> requirements) {
        return Requirements.builder().require(requirements).build();
    }

    static SimpleRequirements single(Requirement requirement) {
        return single(requirement.getPath(), requirement.getRequirement());
    }

    static SimpleRequirements single(String path, Requirement.Spec spec) {
        return new SimpleRequirements(singletonMap(path, spec));
    }

    static Builder builder() {
        return new Builder();
    }

    class Builder {
        private List<Requirement> requireList;
        private List<SimpleRequirements> nestedRequirements;
        private boolean forceList;

        public Builder require(Requirement... requirements) {
            return require(Arrays.asList(requirements));
        }

        public Builder require(Iterable<Requirement> requirements) {
            if (requireList == null) {
                requireList = new ArrayList<>();
            }
            for (Requirement requirement : requirements) {
                requireList.add(requirement);
            }
            return this;
        }

        public Builder require(Requirements.Builder requirements) {
            return require(requirements.build());
        }

        public Builder require(Requirements requirements) {
            if (requirements == null) {
                return this;
            }

            if (nestedRequirements == null) {
                nestedRequirements = new ArrayList<>();
            }
            if (requirements instanceof SimpleRequirements) {
                nestedRequirements.add((SimpleRequirements) requirements);
            } else if (requirements instanceof RequirementsList) {
                forceList = true;
                nestedRequirements.addAll(((RequirementsList) requirements).getRequirements());
            }
            return this;
        }

        @JsonValue
        public Requirements build() {
            Requirements byRequireList = buildByRequireList();
            if (byRequireList != null) {
                require(byRequireList);
            }
            if (nestedRequirements == null) {
                return null;
            }
            if (nestedRequirements.size() == 1 && !forceList) {
                return nestedRequirements.get(0);
            }
            return new RequirementsList(nestedRequirements);
        }

        private Requirements buildByRequireList() {
            if (this.requireList == null) {
                return null;
            }
            return tryBuildUnique(this.requireList)
                    .orElseGet(() -> buildForNonUnique(this.requireList));
        }

        private Optional<Requirements> tryBuildUnique(List<Requirement> requirements) {
            Map<String, Requirement.Spec> result = new HashMap<>();
            for (Requirement requirement : requirements) {
                Requirement.Spec previous = result.put(requirement.getPath(), requirement.getRequirement());
                if (previous != null) {
                    return Optional.empty();
                }
            }
            return Optional.of(new SimpleRequirements(result));
        }

        private Requirements buildForNonUnique(List<Requirement> requirements) {
            ArrayList<SimpleRequirements> result = new ArrayList<>();
            for (Requirement requirement : requirements) {
                result.add(single(requirement));
            }
            return new RequirementsList(result);
        }
    }
}