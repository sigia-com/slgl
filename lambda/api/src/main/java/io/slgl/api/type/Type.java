package io.slgl.api.type;

import com.google.common.collect.ImmutableList;
import io.slgl.api.ExecutionContext;
import io.slgl.api.domain.UploadedFile;
import io.slgl.api.error.ApiException;
import io.slgl.api.model.PermissionEntity;
import io.slgl.api.model.TemplateEntity;
import io.slgl.api.model.TypeEntity;
import io.slgl.api.model.TypeEntity.AnchorEntity;
import io.slgl.api.permission.service.PermissionsMerger;
import io.slgl.api.protocol.NodeRequest;
import io.slgl.api.repository.NodeEntity;
import io.slgl.api.repository.NodeRepository;
import io.slgl.api.service.LinksGetter;
import io.slgl.api.utils.ErrorCode;
import io.slgl.api.utils.PathPrefix;
import io.slgl.api.utils.json.UncheckedObjectMapper;
import io.slgl.api.validator.ValidatorService;
import io.slgl.template.TemplateMatcher;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Objects.equal;
import static io.slgl.api.utils.CollectionUtils.nullToEmptyList;
import static java.util.Collections.emptyList;

@Slf4j
public class Type {

    private final TypeFactory typeFactory = ExecutionContext.get(TypeFactory.class);
    private final NodeRepository nodeRepository = ExecutionContext.get(NodeRepository.class);
    private final LinksGetter linksGetter = ExecutionContext.get(LinksGetter.class);
    private final PermissionsMerger permissionsMerger = ExecutionContext.get(PermissionsMerger.class);
    private final TypeMatcher typeMatcher = ExecutionContext.get(TypeMatcher.class);
    private final ValidatorService validatorService = ExecutionContext.get(ValidatorService.class);
    private final TypeCache typeCache = ExecutionContext.get(TypeCache.class);

    private final String id;
    private final TypeEntity entity;

    private List<String> stateProperties;
    private List<Anchor> anchors;
    private List<TemplateEntity> templates;
    private List<PermissionEntity> permissions;

    public Type(String id, TypeEntity entity) {
        this.id = id;
        this.entity = entity;

        typeCache.putType(this);
    }

    public void validate(NodeRequest request) {
        validate(request, PathPrefix.empty());
    }

    public void validate(NodeRequest request, PathPrefix pathPrefix) {
        Class<?> nativeTypeClass = getNativeTypeClass();

        if (nativeTypeClass != null) {
            Object nativeObject = UncheckedObjectMapper.MAPPER.convertValue(request.getData(), nativeTypeClass);
            validatorService.validate(nativeObject, pathPrefix);
        }

        validateInlineLinks(request, pathPrefix);
    }

    private Class<?> getNativeTypeClass() {
        Class<?> nativeTypeClass = BuiltinType.findById(id)
                .map(BuiltinType::getEntityClass)
                .orElse(null);

        if (nativeTypeClass == null) {
            Type parentType = getParentType();
            if (parentType != null) {
                nativeTypeClass = parentType.getNativeTypeClass();
            }
        }

        return nativeTypeClass;
    }

    private void validateInlineLinks(NodeRequest request, PathPrefix pathPrefix) {
        for (String anchorId : request.getInlineLinks().keySet()) {
            Anchor anchor = getAnchor(anchorId);
            if (anchor == null) {
                throw new ApiException(ErrorCode.LINKING_TARGET_ANCHOR_NOT_FOUND, anchorId);
            }

            List<NodeRequest> inlineLinks = request.getInlineLinks().get(anchorId);
            if (inlineLinks != null) {
                if (anchor.getMaxSize() != null && inlineLinks.size() > anchor.getMaxSize()) {
                    throw new ApiException(ErrorCode.PERMISSION_DENIED);
                }

                int index = 0;
                for (NodeRequest inlineLink : inlineLinks) {
                    Type linkType = typeFactory.get(inlineLink, anchor);

                    linkType.validateCanLinkToAnchor(anchor);
                    linkType.validate(inlineLink, pathPrefix.append(anchorId, index));

                    index++;
                }
            }
        }
    }

    public void validateCanLinkToAnchor(Anchor anchor) {
        if (!typeMatcher.isEqualOrExtendingType(anchor.getType().orElse(null), this)) {
            throw new ApiException(ErrorCode.LINKING_NODE_TYPE_NOT_MATCHING_ANCHOR_TYPE);
        }
    }

    public void validateFile(UploadedFile uploadedFile) {
        if (equal(id, BuiltinType.BASE.getId()) || !hasTemplates()) {
            return;
        }

        if (!uploadedFile.isPdf()) {
            throw new ApiException(ErrorCode.LINKED_DOCUMENT_MUST_BE_PDF);
        }

        var text = uploadedFile.getDocumentText().get();
        var requestObjectValues = uploadedFile.getRequestObject().getData();
        for (TemplateEntity entity : getTemplates()) {
            var template = new TemplateMatcher(entity.toClientDTO());
            try {
                boolean matching = template.isMatching(text, requestObjectValues);
                if (matching) {
                    return;
                }
            } catch (Exception ignored) {
            }
        }

        throw new ApiException(ErrorCode.PERMISSION_DENIED);
    }

    public boolean hasTemplates() {
        return !getTemplates().isEmpty();
    }

    public List<TemplateEntity> getTemplates() {
        if (templates == null) {
            templates = loadTemplates();
        }

        return templates;
    }

    private List<TemplateEntity> loadTemplates() {
        if (id == null || isBuiltinType()) {
            var inlineTemplates = entity.getTemplates();
            return inlineTemplates != null ? inlineTemplates : emptyList();
        }
        NodeEntity typeNode = nodeRepository.readById(id);
        if (typeNode == null) {
            return emptyList();
        }
        return linksGetter.getTemplates(typeNode);
    }

    private boolean isBuiltinType() {
        return BuiltinType.findById(id).isPresent();
    }

    public List<String> getStateProperties() {
        if (stateProperties != null) {
            return stateProperties;
        }

        List<String> stateProperties = new ArrayList<>();

        if (entity.getStateProperties() != null) {
            stateProperties.addAll(entity.getStateProperties());
        }
        if (entity.getAnchors() != null) {
            stateProperties.addAll(entity.getAnchors().stream().map(AnchorEntity::getId).collect(Collectors.toList()));
        }

        Type parentType = getParentType();
        if (parentType != null) {
            stateProperties.addAll(parentType.getStateProperties());
        }

        this.stateProperties = ImmutableList.copyOf(stateProperties);
        return this.stateProperties;
    }

    public List<Anchor> getAnchors() {
        if (this.anchors != null) {
            return this.anchors;
        }

        Map<String, Anchor> anchors = new LinkedHashMap<>();
        buildAnchors(anchors);

        this.anchors = ImmutableList.copyOf(anchors.values());
        return this.anchors;
    }

    private void buildAnchors(Map<String, Anchor> anchors) {
        Type parentType = getParentType();
        if (parentType != null) {
            parentType.buildAnchors(anchors);
        }

        for (AnchorEntity anchor : nullToEmptyList(entity.getAnchors())) {
            anchors.put(anchor.getId(), new Anchor(anchor));
        }
    }

    public Anchor getAnchor(String anchorId) {
        for (Anchor anchor : getAnchors()) {
            if (equal(anchor.getId(), anchorId)) {
                return anchor;
            }
        }

        return null;
    }

    public List<PermissionEntity> getPermissions() {
        if (this.permissions != null) {
            return this.permissions;
        }

        List<PermissionEntity> permissions = new ArrayList<>();

        if (entity.getPermissions() != null) {
            permissions.addAll(entity.getPermissions());
        }

        Type parentType = getParentType();
        if (parentType != null) {
            permissions.addAll(parentType.getPermissions());
        }

        this.permissions = ImmutableList.copyOf(permissionsMerger.mergeExtendingPermissions(permissions));
        return this.permissions;
    }

    public Stream<Type> getTypeHierarchy() {
        return Stream.concat(
                Stream.of(this),
                getParentTypeOpt().stream().flatMap(Type::getTypeHierarchy)
        );
    }

    public Type getParentType() {
        if (entity.getExtendsType() != null) {
            return typeFactory.get(entity.getExtendsType());
        }

        if (!equal(id, BuiltinType.BASE.getId())) {
            return typeFactory.get(BuiltinType.BASE);
        }

        return null;
    }

    private Optional<Type> getParentTypeOpt() {
        return Optional.ofNullable(getParentType());
    }

    public String getId() {
        return id;
    }

    public boolean isType() {
        return equal(id, BuiltinType.TYPE.getId());
    }

    public boolean isOrExtendsType(BuiltinType builtinType) {
        return isOrExtendsType(builtinType.getId());
    }

    public boolean isOrExtendsType(String typeId) {
        return getTypeHierarchy()
                .map(Type::getId)
                .anyMatch(Predicate.isEqual(typeId));
    }
}
