package io.slgl.api.type;

import com.google.common.base.Preconditions;
import io.slgl.api.ExecutionContext;
import io.slgl.api.camouflage.model.CamouflageData;
import io.slgl.api.camouflage.service.CamouflageHelper;
import io.slgl.api.error.ApiException;
import io.slgl.api.model.TypeEntity;
import io.slgl.api.protocol.NodeRequest;
import io.slgl.api.repository.NodeEntity;
import io.slgl.api.repository.NodeRepository;
import io.slgl.api.service.StateService;
import io.slgl.api.utils.ErrorCode;
import io.slgl.api.utils.json.UncheckedObjectMapper;
import io.slgl.client.node.NodeTypeId;

import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Objects.equal;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class TypeFactory {

    private final NodeRepository nodeRepository = ExecutionContext.get(NodeRepository.class);
    private final StateService stateService = ExecutionContext.get(StateService.class);
    private final TypeCache typeCache = ExecutionContext.get(TypeCache.class);

    public Type get(NodeEntity node) {
        if (equal(node.getType(), BuiltinType.CAMOUFLAGE.getId())) {
            return getCamouflagedType(node);
        }

        if (node.getType() instanceof String) {
            return get((String)node.getType());
        }

        Map<String, Object> state = stateService.getStateMap(node).orElse(emptyMap());
        if (state.containsKey("@type")) {
            return getInlineType(state.get("@type"));
        }

        return getBaseType();
    }

    public Type get(NodeRequest object) {
        if (object.getType() != null) {
            return get(object.getType());
        }

        if (object.getInlineType() != null) {
            return getInlineType(object.getInlineType());
        }

        return getBaseType();
    }

    public Type get(NodeRequest object, Anchor anchor) {
        if (object.getType() == null && object.getInlineType() == null && anchor != null) {
            Optional<Type> anchorType = anchor.getType();
            if (anchorType.isPresent()) {
                return anchorType.get();
            }
        }

        return get(object);
    }

    public Type get(BuiltinType builtinType) {
        return get(builtinType.getId());
    }

    public Type get(String typeId) {
        Preconditions.checkNotNull(typeId);

        Type cachedType = typeCache.getType(typeId).orElse(null);
        if (cachedType != null) {
            return cachedType;
        }

        Optional<BuiltinType> nativeType = BuiltinType.findById(typeId);
        if (nativeType.isPresent()) {
            return nativeType.get().getType();
        }

        if(equal(typeId, BuiltinType.CAMOUFLAGE.getId())) {
            throw new ApiException(ErrorCode.CAMOUFLAGED_TYPE_IS_NOT_READABLE);
        }

        NodeEntity typeEntry = nodeRepository.readById(typeId);
        if (typeEntry == null) {
            throw new ApiException(ErrorCode.TYPE_DOESNT_EXIST);
        }

        Type typeOfType = get(typeEntry);
        if (!typeOfType.isOrExtendsType(BuiltinType.TYPE)) {
            throw new ApiException(ErrorCode.TYPE_IS_NOT_TYPE);
        }

        TypeEntity typeEntity;
        if (isNotBlank(typeEntry.getStateSha3())) {
            typeEntity = stateService.getState(typeEntry, TypeEntity.class)
                    .orElseThrow(() -> new ApiException(ErrorCode.STATE_REQUIRED, typeEntry.getId()));
        } else {
            typeEntity = new TypeEntity();
        }

        return new Type(typeId, typeEntity);
    }

    public Type getInlineType(Object inlineType) {
        TypeEntity typeEntity = UncheckedObjectMapper.MAPPER.convertValue(inlineType, TypeEntity.class);

        return new Type(null, typeEntity);
    }

    private Type getCamouflagedType(NodeEntity entry) {
        Object camouflageType = CamouflageHelper.extractCamouflageData(stateService.getStateMap(entry))
                .map(CamouflageData::getCamouflagedType)
                .orElse(null);

        if (camouflageType == null) {
            return getBaseType();
        }

        if (camouflageType instanceof String) {
            return get((String) camouflageType);
        } else {
            return getInlineType(camouflageType);
        }
    }

    public Type create(NodeRequest request) {
        TypeEntity typeEntity = UncheckedObjectMapper.MAPPER.convertValue(request.getData(), TypeEntity.class);
        return new Type(request.getId(), typeEntity);
    }

    public Type create(TypeEntity typeEntity) {
        return new Type(null, typeEntity);
    }

    public Type getBaseType() {
        return get(BuiltinType.BASE);
    }

    public Type getPublicTypeOfNode(String id) {
        var parentNode = nodeRepository.readById(id);
        var parentTypeId = UncheckedObjectMapper.MAPPER.convertValue(parentNode.getType(), NodeTypeId.class);

        if (parentTypeId == null) {
            return getBaseType();
        }
        if (parentTypeId instanceof NodeTypeId.SimpleId) {
            return get(((NodeTypeId.SimpleId) parentTypeId).getType());
        }
        if (parentTypeId instanceof NodeTypeId.ExtendsId) {
            return get(((NodeTypeId.ExtendsId) parentTypeId).getExtendsType());
        }
        throw new UnsupportedOperationException("unknown node type implementation: " + parentTypeId.getClass());
    }
}
