package io.slgl.api.observer.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.slgl.api.ExecutionContext;
import io.slgl.api.domain.Link;
import io.slgl.api.error.ApiException;
import io.slgl.api.observer.model.ObserverEntity;
import io.slgl.api.observer.model.S3Storage;
import io.slgl.api.protocol.LinkRequest;
import io.slgl.api.protocol.NodeRequest;
import io.slgl.api.service.LinksGetter;
import io.slgl.api.type.BuiltinType;
import io.slgl.api.utils.ErrorCode;
import io.slgl.api.utils.json.UncheckedObjectMapper;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static com.google.common.base.Objects.equal;

public class ObserverValidator {

    private final LinksGetter linksGetter = ExecutionContext.get(LinksGetter.class);

    public void validateInLineObservers(NodeRequest request) {
        List<NodeRequest> inlineObservers = request.getInlineLinks().get("#observers");

        if (inlineObservers == null || inlineObservers.isEmpty()) {
            return;
        }

        ObserverPaths observerPaths = new ObserverPaths();
        for (NodeRequest inlineObserver : inlineObservers) {
            ObserverEntity observer = UncheckedObjectMapper.MAPPER.convertValue(inlineObserver.getData(), ObserverEntity.class);
            observerPaths.addPaths(observer);
        }

        observerPaths.validatePaths();
    }

    public void validateObserverAgainstParentObservers(Link link, LinkRequest request) {
        if (!equal(request.getTargetAnchor(), "#observers")
                || !link.getLinkSourceType().isOrExtendsType(BuiltinType.OBSERVER)) {
            return;
        }

        List<ObserverEntity> observers = linksGetter.getObservers(link.getLinkTarget());

        ObserverPaths observerPaths = new ObserverPaths();
        for (ObserverEntity observer : observers) {
            observerPaths.addPaths(observer);
        }

        observerPaths.validatePaths();
    }

    @Getter
    private static class ObserverPaths {

        private final List<String> recoveryPaths = Lists.newArrayList();
        private final List<String> storagePaths = Lists.newArrayList();

        public void addPaths(ObserverEntity observer) {
            if (Objects.nonNull(observer.getRecoveryStorage())) {
                recoveryPaths.add(observer.getRecoveryStorage().getPath());
            }
            if (observer.hasStorage()) {
                if (observer.getStorage() instanceof S3Storage) {
                    storagePaths.add(String.format("aws_s3/%s/%s", observer.getS3Storage().getBucket(), observer.getS3Storage().getPath()));
                } else {
                    throw new IllegalStateException(String.format("Unknown storage type %s:", observer.getStorage().getClass().getCanonicalName()));
                }
            }
        }

        public void validatePaths() {
            if (recoveryPaths.size() != Sets.newHashSet(recoveryPaths).size()) {
                throw new ApiException(ErrorCode.OBSERVER_UNIQUE_RECOVERY_STORAGE_PATHS, duplicatedPaths(recoveryPaths));
            }
            if (storagePaths.size() != Sets.newHashSet(storagePaths).size()) {
                throw new ApiException(ErrorCode.OBSERVER_UNIQUE_STORAGE_LOCATIONS, duplicatedPaths(storagePaths));
            }
        }

        private String duplicatedPaths(List<String> paths) {
            return paths
                    .stream()
                    .collect(Collector.of(
                            (Supplier<HashMap<String, Integer>>) HashMap::new,
                            (m, s) -> {
                                if (m.containsKey(s)) {
                                    m.put(s, m.get(s) + 1);
                                } else {
                                    m.put(s, 1);
                                }
                            },
                            (map1, map2) -> {
                                map1.putAll(map2);
                                return map1;
                            }
                    ))
                    .entrySet()
                    .stream()
                    .filter(e -> e.getValue() > 1)
                    .map(Entry::getKey)
                    .collect(Collectors.joining(", "));
        }
    }
}
