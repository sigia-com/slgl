package io.slgl.api.permission.service;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import io.slgl.api.error.ApiException;
import io.slgl.api.model.PermissionEntity;
import io.slgl.api.utils.ErrorCode;
import io.slgl.client.jsonlogic.And;

import java.util.*;
import java.util.stream.Collectors;

import static io.slgl.api.utils.CollectionUtils.nullToEmptyList;

public class PermissionsMerger {

    public List<PermissionEntity> mergeExtendingPermissions(List<PermissionEntity> permissions) {
        Multimap<String, PermissionEntity> permissionByExtendsId = getExtendingPermissions(permissions);

        List<PermissionEntity> result = new ArrayList<>();

        for (PermissionEntity permission : getBasePermissions(permissions)) {
            Collection<PermissionEntity> extendingPermissions = permissionByExtendsId.get(permission.getId());

            if (extendingPermissions.isEmpty()) {
                result.add(permission);

            } else {
                PermissionEntity mergedPermission = new PermissionEntity()
                        .setId(permission.getId())
                        .setAllow(permission.getAllow())
                        .setEvaluateStateAccessAsUser(permission.getEvaluateStateAccessAsUser())
                        .setRequire(mergeRequirements(permission, extendingPermissions))
                        .setRequireLogic(mergerRequireLogic(permission, extendingPermissions));

                result.add(mergedPermission);
            }
        }

        return result;
    }

    private PermissionEntity.Requirements mergeRequirements(PermissionEntity permission, Collection<PermissionEntity> extendingPermissions) {
        List<Map<String, PermissionEntity.Requirement>> requirements = new ArrayList<>();

        if (permission.getRequire() != null) {
            requirements.addAll(permission.getRequire().getRequirements());
        }
        for (PermissionEntity extendingPermission : extendingPermissions) {
            if (extendingPermission.getRequire() != null) {
                requirements.addAll(extendingPermission.getRequire().getRequirements());
            }
        }

        if (requirements.isEmpty()) {
            return null;
        }

        if (requirements.size() == 1) {
            return PermissionEntity.Requirements.simple(requirements.get(0));
        }

        return PermissionEntity.Requirements.list(requirements);
    }

    private Object mergerRequireLogic(PermissionEntity permission, Collection<PermissionEntity> extendingPermissions) {
        List<Object> requireLogicObjects = new ArrayList<>();
        if (permission.getRequireLogic() != null) {
            requireLogicObjects.add(permission.getRequireLogic());
        }
        for (PermissionEntity extendingPermission : extendingPermissions) {
            if (extendingPermission.getRequireLogic() != null) {
                requireLogicObjects.add(extendingPermission.getRequireLogic());
            }
        }

        if (requireLogicObjects.isEmpty()) {
            return null;
        }

        if (requireLogicObjects.size() == 1) {
            return requireLogicObjects.get(0);
        }

        return new And(requireLogicObjects.toArray());
    }

    public void validatePermissions(List<PermissionEntity> typePermissions, List<PermissionEntity> allPermissions) {
        Set<String> permissionIds = getBasePermissions(allPermissions).stream()
                .map(PermissionEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (PermissionEntity permission : typePermissions) {
            if (permission.getExtendsId() != null && !permissionIds.contains(permission.getExtendsId())) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR_WITH_MESSAGE, "'extends_id' refers to not existing permission id: " + permission.getExtendsId());
            }
        }

        Set<PermissionEntity> parentPermissions = new HashSet<>(allPermissions);
        parentPermissions.removeAll(typePermissions);

        List<PermissionEntity.Allow> parentAllows = parentPermissions.stream()
                .flatMap(p -> nullToEmptyList(p.getAllow()).stream())
                .collect(Collectors.toList());

        for (PermissionEntity permission : typePermissions) {
            if (permission.getExtendsId() == null) {
                for (PermissionEntity.Allow allow : nullToEmptyList(permission.getAllow())) {
                    for (PermissionEntity.Allow parentAllow : parentAllows) {
                        if (parentAllow.doesAllow(allow)) {
                            throw new ApiException(
                                    ErrorCode.VALIDATION_ERROR_WITH_MESSAGE,
                                    "Must not redefine allowed action from permission from extending type: " +
                                            "action=" + allow.getAction() +
                                            (allow.getAnchor() != null ? ", anchor=" + allow.getAnchor() : ""));
                        }
                    }
                }
            }
        }

    }

    private List<PermissionEntity> getBasePermissions(List<PermissionEntity> permissions) {
        return permissions.stream()
                .filter(permission -> permission.getExtendsId() == null)
                .collect(Collectors.toList());
    }

    private Multimap<String, PermissionEntity> getExtendingPermissions(List<PermissionEntity> permissions) {
        Multimap<String, PermissionEntity> permissionByExtendsId = LinkedHashMultimap.create();

        for (PermissionEntity permission : permissions) {
            if (permission.getExtendsId() != null) {
                permissionByExtendsId.put(permission.getExtendsId(), permission);
            }
        }

        return permissionByExtendsId;
    }
}
