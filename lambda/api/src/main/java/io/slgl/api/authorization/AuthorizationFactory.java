package io.slgl.api.authorization;

import com.google.common.base.Preconditions;
import io.slgl.api.ExecutionContext;
import io.slgl.api.authorization.model.AuthorizationEntity;
import io.slgl.api.error.ApiException;
import io.slgl.api.repository.NodeEntity;
import io.slgl.api.repository.NodeRepository;
import io.slgl.api.service.StateService;
import io.slgl.api.type.BuiltinType;
import io.slgl.api.type.TypeFactory;
import io.slgl.api.utils.ErrorCode;

public class AuthorizationFactory {

    private final NodeRepository nodeRepository = ExecutionContext.get(NodeRepository.class);
    private final StateService stateService = ExecutionContext.get(StateService.class);

    private final TypeFactory typeFactory = ExecutionContext.get(TypeFactory.class);

    public Authorization get(String authorizationId) {
        Preconditions.checkNotNull(authorizationId);

        NodeEntity authorizationNode = nodeRepository.readById(authorizationId);
        if (authorizationNode == null) {
            throw new ApiException(ErrorCode.AUTHORIZATION_DOESNT_EXIST);
        }

        if (!typeFactory.get(authorizationNode).isOrExtendsType(BuiltinType.AUTHORIZATION)) {
            throw new ApiException(ErrorCode.AUTHORIZATION_HAS_INVALID_TYPE);
        }

        AuthorizationEntity authorizationEntity = stateService.getState(authorizationNode, AuthorizationEntity.class)
                .orElseThrow(() -> new IllegalStateException("State must not be empty: " + authorizationNode.getId()));

        return new Authorization(authorizationNode, authorizationEntity);
    }
}
