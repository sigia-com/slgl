package io.slgl.client.node.permission;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RequirementsUtil {

    public static Map<String, Requirement.Spec> toMap(Collection<Requirement> requirements) {
        return requirements.stream().collect(Collectors.toMap(
                Requirement::getPath,
                Requirement::getRequirement,
                (u, v) -> {
                    throw new IllegalStateException(String.format("Duplicate key %s", u));
                },
                LinkedHashMap::new
        ));
    }

    public static List<Requirement> toList(Map<String, Requirement.Spec> requirements) {
        return requirements.entrySet()
                .stream()
                .map(it -> new Requirement(it.getKey(), it.getValue()))
                .collect(Collectors.toList());
    }

}
