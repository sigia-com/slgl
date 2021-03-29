package io.slgl.api.authorization;

import com.google.common.base.Objects;
import io.slgl.api.ExecutionContext;
import io.slgl.api.authorization.model.AuthorizationEntity;
import io.slgl.api.authorization.model.Authorize;
import io.slgl.api.context.EvaluationContextBuilder;
import io.slgl.api.error.ApiException;
import io.slgl.api.model.PermissionEntity;
import io.slgl.api.permission.PermissionChecker;
import io.slgl.api.permission.PermissionCheckerContext;
import io.slgl.api.permission.service.AuditorNotifier;
import io.slgl.api.repository.NodeEntity;
import io.slgl.api.utils.ErrorCode;
import io.slgl.client.audit.RequestType;
import io.slgl.permission.context.EvaluationContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static io.slgl.api.utils.CollectionUtils.nullToEmptyList;

@Slf4j
public class Authorization {

    private final AuditorNotifier auditorNotifier = ExecutionContext.get(AuditorNotifier.class);

    private final NodeEntity entry;
    private final AuthorizationEntity entity;

    public Authorization(NodeEntity entry, AuthorizationEntity entity) {
        this.entry = entry;
        this.entity = entity;
    }

    public void verifyPermissions(EvaluationContextBuilder contextBuilder, Authorization authorization) {
        var permissionChecker = new PermissionChecker(RequestType.USE_AUTHORIZATION, entry.getId(), authorization);

        try {
            PermissionCheckerContext.executeWithContext(permissionChecker, (unused) -> {
                EvaluationContext context = contextBuilder.withAuthorization(authorization).build();

                PermissionEntity permission = new PermissionEntity()
                        .setRequire(entity.getRequire())
                        .setRequireLogic(entity.getRequireLogic());

                permissionChecker.verifyUseAuthorizationPermission(context, permission, entry.getId());
            });

        } catch (ApiException e) {
            if (e.getErrorCode() == ErrorCode.PERMISSION_DENIED) {
                throw new ApiException(ErrorCode.AUTHORIZATION_PERMISSION_DENIED, e);
            } else {
                throw e;
            }

        } finally {
            auditorNotifier.notify(entry, permissionChecker.buildPermissionAudit());
        }
    }

    public List<Object> getAuthorizationPrincipals() {
        return entity.getAuthorizationPrincipals();
    }

    public void verifyIsMatchingAction(Authorize authorizeForAction) {
        if (authorizeForAction == null) {
            throw new ApiException(ErrorCode.AUTHORIZATION_NOT_MATCHING_ACTION);
        }

        for (Authorize authorize : nullToEmptyList(entity.getAuthorize())) {
            if (Objects.equal(authorize, authorizeForAction)) {
                return;
            }
        }

        throw new ApiException(ErrorCode.AUTHORIZATION_NOT_MATCHING_ACTION);
    }
}
