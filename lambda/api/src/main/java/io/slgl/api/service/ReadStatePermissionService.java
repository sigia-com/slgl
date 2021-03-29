package io.slgl.api.service;

import io.slgl.api.ExecutionContext;
import io.slgl.api.context.EvaluationContextBuilder;
import io.slgl.api.domain.ApiUser;
import io.slgl.api.error.ApiException;
import io.slgl.api.permission.PermissionChecker;
import io.slgl.api.permission.PermissionCheckerContext;
import io.slgl.api.repository.NodeEntity;
import io.slgl.api.type.Type;
import io.slgl.api.type.TypeFactory;
import io.slgl.api.utils.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ReadStatePermissionService implements ExecutionContext.PreExecutionCallback, ExecutionContext.PostExecutionCallback {

    private final Map<NodeAndUser, Boolean> cache = new HashMap<>();
    private final Set<String> nodesWithStateFromRequest = new HashSet<>();

    public void validateStateAccess(NodeEntity node, ApiUser user) {
        if (nodesWithStateFromRequest.contains(node.getId())) {
            return;
        }

        NodeAndUser cacheKey = new NodeAndUser(node, user);
        Boolean valueFromCache = cache.get(cacheKey);

        if (valueFromCache == null) {
            try {
                verifyReadStatePermissions(node, user);
                cache.put(cacheKey, true);
                return;

            } catch (Exception e) {
                cache.put(cacheKey, false);
                throw e;
            }
        }

        if (!valueFromCache) {
            throw new ApiException(ErrorCode.PERMISSION_DENIED);
        }
    }

    public void addNodeWithStateFromRequest(String nodeId) {
        nodesWithStateFromRequest.add(nodeId);
    }

    private void verifyReadStatePermissions(NodeEntity node, ApiUser user) {
        PermissionChecker permissionChecker = PermissionCheckerContext.get();

        var context = new EvaluationContextBuilder()
                .withNodeObject(node)
                .withPrincipal(user != null ? user.asPrincipal(): null)
                .withAuthorization(permissionChecker.getAuthorization())
                .build();

        Type type = ExecutionContext.get(TypeFactory.class).get(node);

        permissionChecker.verifyReadStatePermission(type, context, node.getId());
    }

    @Override
    public void beforeExecution() {
        clearCache();
    }

    @Override
    public void afterExecution() {
        clearCache();
    }

    private void clearCache() {
        cache.clear();
        nodesWithStateFromRequest.clear();
    }

    @AllArgsConstructor
    @EqualsAndHashCode
    private static class NodeAndUser {
        private final NodeEntity node;
        private final ApiUser user;
    }
}
