package io.slgl.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import io.slgl.api.ExecutionContext;
import io.slgl.api.camouflage.model.Camouflage;
import io.slgl.api.domain.ApiUser;
import io.slgl.api.protocol.NodeRequest;
import io.slgl.api.repository.NodeEntity;
import io.slgl.api.repository.StateRepository;
import io.slgl.api.type.Type;
import io.slgl.api.utils.json.UncheckedObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.slgl.api.utils.Utils.generateSha3Salt;
import static io.slgl.api.utils.json.UncheckedObjectMapper.MAPPER;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

@Slf4j
public class StateService {

    private final StateRepository stateRepository = ExecutionContext.get(StateRepository.class);
    private final ReadStatePermissionService readStatePermissionService = ExecutionContext.get(ReadStatePermissionService.class);
    private final StateCache stateCache = ExecutionContext.get(StateCache.class);

    public void saveState(String nodeId, Map<String, Object> state) {
        stateCache.put(nodeId, state);
        readStatePermissionService.addNodeWithStateFromRequest(nodeId);
    }

    public Map<String, Object> buildStateMap(NodeRequest request, Type objectType) {
        Map<String, Object> state = new LinkedHashMap<>();

        if (objectType != null) {
            var stringObjectMap = request.toMap();
            List<String> stateProperties = objectType.getStateProperties();

            for (Entry<String, Object> entry : stringObjectMap.entrySet()) {
                if (stateProperties.contains(entry.getKey())) {
                    state.put(entry.getKey(), entry.getValue());
                }
            }
        }

        Camouflage camouflage = request.getCamouflage();
        if (camouflage != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> camouflageData = MAPPER.convertValue(camouflage, Map.class);

            if (request.getInlineType() != null) {
                camouflageData.put("camouflaged_type", request.getInlineType());
            }
            if (request.getType() != null) {
                camouflageData.put("camouflaged_type", request.getType());
            }
            state.put("@camouflage", camouflageData);
        } else {
            if (request.getInlineType() != null) {
                state.put("@type", request.getInlineType());
            }
        }

        if (!state.isEmpty()) {
            state.put("@salt", generateSha3Salt());
        }

        return state;
    }

    public Optional<Map<String, Object>> getStateMap(NodeEntity entry) {
        var state = stateCache.get(entry.getId());

        if (state.isEmpty()) {
            state = stateRepository.readState(entry);

            if (state.isPresent()) {
                stateCache.put(entry.getId(), state.get());
            }
        }

        return state;
    }

    public <T> Optional<T> getState(NodeEntity entry, Class<T> modelClass) {
        return getStateMap(entry)
                .map(state -> {
                    Map<String, Object> filteredState = state.entrySet().stream()
                            .filter(e -> !e.getKey().startsWith("@") && !e.getKey().startsWith("#"))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                    return UncheckedObjectMapper.MAPPER.convertValue(filteredState, modelClass);
                });
    }

    private Optional<String> getStateProperty(NodeEntity entry, String fieldName) {
        return getStateMap(entry)
                .flatMap(state -> state.containsKey(fieldName) ? ofNullable(state.get(fieldName)) : empty())
                .map(MAPPER::writeValueAsString);
    }

    public <T> Optional<T> getStateProperty(NodeEntity entry, String fieldName, Class<T> type) {
        return getStateProperty(entry, fieldName)
                .map(valueJson -> MAPPER.readValue(valueJson, type));
    }

    public <T> Optional<T> getStateProperty(NodeEntity entry, String fieldName, TypeReference<T> valueTypeRef) {
        return getStateProperty(entry, fieldName)
                .map(valueJson -> MAPPER.readValue(valueJson, valueTypeRef));
    }

    public <T> List<T> getInlineLinks(NodeEntity entry, String anchorName, Class<T> entityClass) {
        String inlineLinksJson = getStateProperty(entry, anchorName).orElse(null);
        if (inlineLinksJson == null) {
            return emptyList();
        }

        JavaType type = MAPPER.getTypeFactory().constructParametricType(List.class,
                MAPPER.getTypeFactory().constructParametricType(Map.class, String.class, Object.class));

        List<Map<String, Object>> inlineLinks = MAPPER.readValue(inlineLinksJson, type);

        return inlineLinks.stream()
                .map(stateMap -> {
                    Map<String, Object> filteredStateMap = stateMap.entrySet().stream()
                            .filter(e -> !e.getKey().startsWith("@") && !e.getKey().startsWith("#"))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                    return MAPPER.convertValue(filteredStateMap, entityClass);
                })
                .collect(Collectors.toList());
    }

    public void validateStateAccess(NodeEntity entry, ApiUser user) {
        readStatePermissionService.validateStateAccess(entry, user);
    }

    public void addStateFromRequest(Map<String, Map<String, Object>> stateFromRequest) {
        if (stateFromRequest == null) {
            return;
        }

        for (Entry<String, Map<String, Object>> entry : stateFromRequest.entrySet()) {
            String nodeId = entry.getKey();
            Map<String, Object> state = entry.getValue();

            readStatePermissionService.addNodeWithStateFromRequest(nodeId);
            stateRepository.putStateFromRequest(nodeId, state);
        }
    }
}
