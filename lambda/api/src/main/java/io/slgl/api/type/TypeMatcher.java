package io.slgl.api.type;

import com.google.common.collect.*;
import io.slgl.api.model.PermissionEntity;
import io.slgl.api.model.PermissionEntity.Allow;
import io.slgl.api.model.TemplateEntity;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkArgument;
import static io.slgl.api.utils.CollectionUtils.nullToEmptyCollection;
import static io.slgl.api.utils.CollectionUtils.nullToEmptyList;
import static java.util.Collections.emptySet;
import static java.util.function.Function.identity;

@Slf4j
public class TypeMatcher {

    public boolean isEqualOrExtendingType(Type baseType, Type extendingType) {
        if (baseType == null || equal(baseType.getId(), BuiltinType.BASE.getId())) {
            return true;
        }
        if (extendingType == null) {
            return false;
        }
        if (baseType.getId() != null && extendingType.isOrExtendsType(baseType.getId())) {
            return true;
        }

        return isExtendingStateProperties(baseType, extendingType)
                && isExtendingAnchors(baseType, extendingType)
                && isExtendingTemplates(baseType, extendingType)
                && isExtendingPermissions(baseType, extendingType);
    }

    private boolean isExtendingStateProperties(Type baseType, Type extendingType) {
        List<String> baseStateProperties = baseType.getStateProperties();
        Set<String> extendingStateProperties = new HashSet<>(extendingType.getStateProperties());

        return extendingStateProperties.containsAll(baseStateProperties);
    }

    private boolean isExtendingAnchors(Type baseType, Type extendingType) {
        Map<String, Anchor> extendingTypeAnchors = extendingType.getAnchors().stream()
                .collect(Collectors.toMap(Anchor::getId, identity()));

        for (Anchor baseAnchor : baseType.getAnchors()) {
            Anchor extendingAnchor = extendingTypeAnchors.get(baseAnchor.getId());

            if (extendingAnchor == null) {
                return false;
            }

            if (!equal(baseAnchor.getMaxSize(), extendingAnchor.getMaxSize())) {
                return false;
            }

            if (!isEqualOrExtendingType(baseAnchor.getType().orElse(null), extendingAnchor.getType().orElse(null))) {
                return false;
            }
        }

        return true;
    }

    private boolean isExtendingTemplates(Type baseType, Type extendingType) {
        Collection<TemplateEntity> baseTypeTemplates = baseType.getTemplates();
        Set<TemplateEntity> extendingTypeTemplates = new HashSet<>(extendingType.getTemplates());

        return extendingTypeTemplates.containsAll(baseTypeTemplates);
    }

    private boolean isExtendingPermissions(Type baseType, Type extendingType) {
        Multimap<Allow, PermissionEntity> basePermissions = groupPermissionsByAllow(baseType);
        Multimap<Allow, PermissionEntity> extendingPermissions = groupPermissionsByAllow(extendingType);

        for (Allow allow : basePermissions.keySet()) {
            if (!isExtendingPermissionWithSameAllow(basePermissions.get(allow), extendingPermissions.get(allow))) {
                return false;
            }
        }

        return true;
    }

    private boolean isExtendingPermissionWithSameAllow(
            Collection<PermissionEntity> basePermissions,
            Collection<PermissionEntity> extendingPermissions
    ) {
        basePermissions = nullToEmptyCollection(basePermissions);
        extendingPermissions = nullToEmptyCollection(extendingPermissions);

        if (basePermissions.isEmpty()) {
            return true;
        }

        if (basePermissions.size() != extendingPermissions.size()) {
            return false;
        }

        List<PermissionEntity> extendingPermissionsList = ImmutableList.copyOf(extendingPermissions);
        for (List<PermissionEntity> basePermissionsList : Collections2.permutations(basePermissions)) {
            if (isExtendingPermissionWithSameAllowInGivenOrder(basePermissionsList, extendingPermissionsList)) {
                return true;
            }
        }

        return false;
    }

    private boolean isExtendingPermissionWithSameAllowInGivenOrder(
            List<PermissionEntity> basePermissions,
            List<PermissionEntity> extendingPermissions
    ) {
        checkArgument(basePermissions.size() == extendingPermissions.size());

        for (int i = 0; i < basePermissions.size(); i++) {
            PermissionEntity basePermission = basePermissions.get(i);
            PermissionEntity extendingPermission = extendingPermissions.get(i);

            Set<PermissionEntity.PathRequirement> baseRequirements = normalizeRequirement(basePermission.getRequire());
            Set<PermissionEntity.PathRequirement> extendingRequirements = normalizeRequirement(extendingPermission.getRequire());

            if (!extendingRequirements.containsAll(baseRequirements)) {
                return false;
            }

            Set<Object> baseRequireLogic = normalizeRequireLogic(basePermission.getRequireLogic());
            Set<Object> extendingRequireLogic = normalizeRequireLogic(extendingPermission.getRequireLogic());

            if (!extendingRequireLogic.containsAll(baseRequireLogic)) {
                return false;
            }
        }

        return true;
    }

    private Set<PermissionEntity.PathRequirement> normalizeRequirement(PermissionEntity.Requirements requirements) {
        if (requirements == null) {
            return emptySet();
        }

        return Sets.newHashSet(requirements.iterator());
    }

    private Set<Object> normalizeRequireLogic(Object requireLogic) {
        if (requireLogic == null || equal(requireLogic, Boolean.TRUE)) {
            return Collections.emptySet();
        }

        if (requireLogic instanceof Map) {
            Map<?, ?> requireLogicMap = (Map<?, ?>) requireLogic;
            if (requireLogicMap.size() == 1 && requireLogicMap.get("and") instanceof List) {
                List<?> requireLogicRequirements = (List<?>) requireLogicMap.get("and");

                return new HashSet<>(requireLogicRequirements);
            }
        }

        return Collections.singleton(requireLogic);
    }

    private Multimap<Allow, PermissionEntity> groupPermissionsByAllow(Type type) {
        Multimap<Allow, PermissionEntity> permissionByAllow = HashMultimap.create();

        for (PermissionEntity permission : type.getPermissions()) {
            for (Allow allow : nullToEmptyList(permission.getAllow())) {
                permissionByAllow.put(allow, permission);
            }
        }

        return permissionByAllow;
    }
}
