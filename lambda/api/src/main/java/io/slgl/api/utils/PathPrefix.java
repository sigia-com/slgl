package io.slgl.api.utils;

import com.google.common.collect.ImmutableList;
import lombok.Getter;

import java.util.List;

@Getter
public class PathPrefix {

    private final List<Object> pathSegments;

    public PathPrefix(List<Object> pathSegments) {
        this.pathSegments = ImmutableList.copyOf(pathSegments);
    }

    public PathPrefix(Object... pathSegments) {
        this.pathSegments = ImmutableList.copyOf(pathSegments);
    }

    public static PathPrefix empty() {
        return new PathPrefix(ImmutableList.of());
    }

    public PathPrefix append(Object... path) {
        return new PathPrefix(ImmutableList.builder().addAll(pathSegments).add(path).build());
    }
}
