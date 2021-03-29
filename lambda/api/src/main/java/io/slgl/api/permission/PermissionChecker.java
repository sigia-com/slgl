package io.slgl.api.permission;

import com.google.common.collect.ImmutableList;
import io.slgl.api.ExecutionContext;
import io.slgl.api.authorization.Authorization;
import io.slgl.api.domain.ApiUser;
import io.slgl.api.error.ApiException;
import io.slgl.api.model.PermissionEntity;
import io.slgl.api.model.PermissionEntity.Allow;
import io.slgl.api.service.CurrentUserService;
import io.slgl.api.type.Type;
import io.slgl.api.utils.ErrorCode;
import io.slgl.api.utils.json.UncheckedObjectMapper;
import io.slgl.client.audit.*;
import io.slgl.client.node.permission.Permission;
import io.slgl.permission.PermissionProcessor;
import io.slgl.permission.context.EvaluationContext;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import static io.slgl.api.model.PermissionEntity.AllowAction.*;
import static io.slgl.api.utils.CollectionUtils.nullToEmptyList;
import static java.util.stream.Collectors.toList;

public class PermissionChecker {

    private final CurrentUserService currentUserService = ExecutionContext.get(CurrentUserService.class);
    private final PermissionProcessor permissionProcessor = ExecutionContext.get(PermissionProcessor.class);

    private final PermissionAuditBuilder permissionAudit = new PermissionAuditBuilder();
    private final Authorization authorization;

    public PermissionChecker(RequestType requestType, String node, String anchor, Authorization authorization) {
        permissionAudit.node(node)
                .anchor(anchor)
                .requestType(requestType)
                .evaluationTime(Instant.now());

        this.authorization = authorization;
    }

    public PermissionChecker(RequestType requestType, String node, Authorization authorization) {
        this(requestType, node, null, authorization);
    }

    public void verifyLinkPermission(
            Type linkTargetType,
            EvaluationContext context,
            String linkTargetId,
            String linkAnchorId,
            boolean allowWhenNoPermissions
    ) {
        PermissionEvaluationBuilder permissionEvaluation = new PermissionEvaluationBuilder()
                .evaluationType(PermissionEvaluationType.LINK_NODE)
                .node(linkTargetId)
                .anchor(linkAnchorId);
        try {
            List<PermissionEntity> permissions = filterPermissionsMatchingAllow(
                    linkTargetType, allow -> allow.doesAllow(LINK_TO_ANCHOR, linkAnchorId));

            if (permissions.isEmpty()) {
                if (allowWhenNoPermissions) {
                    permissionEvaluation.success(true);
                    return;
                } else {
                    throw new ApiException(ErrorCode.PERMISSION_DENIED);
                }
            }

            verifyPermissions(permissionEvaluation, permissions, context);

        } finally {
            permissionAudit.addEvaluatedPermission(permissionEvaluation.build());
        }
    }

    public void verifyUnlinkPermission(
            Type linkTargetType,
            EvaluationContext context,
            String linkTargetId,
            String linkAnchorId,
            boolean allowWhenNoPermissions
    ) {
        PermissionEvaluationBuilder permissionEvaluation = new PermissionEvaluationBuilder()
                .evaluationType(PermissionEvaluationType.UNLINK_NODE)
                .node(linkTargetId)
                .anchor(linkAnchorId);
        try {
            List<PermissionEntity> permissions = filterPermissionsMatchingAllow(
                    linkTargetType, allow -> allow.doesAllow(UNLINK_FROM_ANCHOR, linkAnchorId));

            if (permissions.isEmpty()) {
                if (allowWhenNoPermissions) {
                    permissionEvaluation.success(true);
                    return;
                } else {
                    throw new ApiException(ErrorCode.PERMISSION_DENIED);
                }
            }

            verifyPermissions(permissionEvaluation, permissions, context);

        } finally {
            permissionAudit.addEvaluatedPermission(permissionEvaluation.build());
        }
    }

    public void verifyReadStatePermission(Type type, EvaluationContext context, String nodeId) {
        PermissionEvaluationBuilder permissionEvaluation = new PermissionEvaluationBuilder()
                .evaluationType(PermissionEvaluationType.READ_STATE)
                .node(nodeId);

        try {
            List<PermissionEntity> permissions = filterPermissionsMatchingAllow(
                    type, allow -> allow.doesAllow(READ_STATE));

            if (permissions.isEmpty()) {
                throw new ApiException(ErrorCode.PERMISSION_DENIED);
            }

            verifyPermissions(permissionEvaluation, permissions, context);

        } finally {
            permissionAudit.addEvaluatedPermission(permissionEvaluation.build());
        }
    }

    public void verifyUseAuthorizationPermission(EvaluationContext context, PermissionEntity permission, String nodeId) {
        PermissionEvaluationBuilder permissionEvaluation = new PermissionEvaluationBuilder()
                .evaluationType(PermissionEvaluationType.USE_AUTHORIZATION)
                .node(nodeId);

        try {
            verifyPermissions(permissionEvaluation, ImmutableList.of(permission), context);

        } finally {
            permissionAudit.addEvaluatedPermission(permissionEvaluation.build());
        }
    }

    private List<PermissionEntity> filterPermissionsMatchingAllow(Type type, Predicate<Allow> predicateOnAllow) {
        return type.getPermissions().stream()
                .filter(permission -> nullToEmptyList(permission.getAllow()).stream().anyMatch(predicateOnAllow))
                .collect(toList());
    }

    private void verifyPermissions(PermissionEvaluationBuilder permissionEvaluation,
                                   Collection<PermissionEntity> permissions, EvaluationContext context) {

        for (PermissionEntity permission : permissions) {
            PermissionEvaluationResult result = processPermission(permission, context);

            permissionEvaluation.addEvaluationResults(result);

            if (result.isSuccess()) {
                permissionEvaluation.success(true);
                return;
            }
        }

        throw new ApiException(ErrorCode.PERMISSION_DENIED);
    }

    private PermissionEvaluationResult processPermission(PermissionEntity permission, EvaluationContext context) {
        ApiUser previousPermissionsUser = currentUserService.getPermissionsUser();
        try {
            if (permission.getEvaluateStateAccessAsUser() != null) {
                currentUserService.setPermissionsUser(new ApiUser(permission.getEvaluateStateAccessAsUser()));
            }
            return permissionProcessor.process(context, UncheckedObjectMapper.MAPPER.convertValue(permission, Permission.class));
        } finally {
            currentUserService.setPermissionsUser(previousPermissionsUser);
        }
    }

    public boolean hasEmptyPermissionAudit() {
        return permissionAudit.isEmpty();
    }

    public PermissionAudit buildPermissionAudit() {
        return permissionAudit.build();
    }

    public void addEvaluationLogAndMarkAsFailed(String code, String details) {
        permissionAudit
                .addEvaluationLog(new EvaluationLogEntry(code, details))
                .forceSuccess(false);
    }

    public Authorization getAuthorization() {
        return authorization;
    }
}
