package io.slgl.api.service;

import io.slgl.api.ExecutionContext;
import io.slgl.api.authorization.Authorization;
import io.slgl.api.authorization.AuthorizationService;
import io.slgl.api.authorization.model.Authorize;
import io.slgl.api.context.EvaluationContextBuilder;
import io.slgl.api.domain.RequestItemObject;
import io.slgl.api.error.ApiException;
import io.slgl.api.permission.PermissionChecker;
import io.slgl.api.permission.PermissionCheckerContext;
import io.slgl.api.permission.service.AuditorNotifier;
import io.slgl.api.protocol.*;
import io.slgl.api.repository.NodeEntity;
import io.slgl.api.repository.NodeRepository;
import io.slgl.api.repository.TransactionManager;
import io.slgl.api.utils.ErrorCode;
import io.slgl.api.utils.PathPrefix;
import io.slgl.api.validator.ValidatorService;
import io.slgl.client.audit.RequestType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.slgl.api.utils.CollectionUtils.nullToEmptyList;
import static io.slgl.api.utils.CollectionUtils.nullToEmptyMap;
import static io.slgl.api.utils.json.UncheckedObjectMapper.MAPPER;
import static java.util.Collections.unmodifiableList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class LedgerService {

    public static final int MAX_BATCH_SIZE = 20;

    private final NodeRepository nodeRepository = ExecutionContext.get(NodeRepository.class);
    private final StateService stateService = ExecutionContext.get(StateService.class);
    private final CurrentUserService currentUserService = ExecutionContext.get(CurrentUserService.class);
    private final TransactionManager transactionManager = ExecutionContext.get(TransactionManager.class);
    private final AuthorizationService authorizationService = ExecutionContext.get(AuthorizationService.class);
    private final AuditorNotifier auditorNotifier = ExecutionContext.get(AuditorNotifier.class);
    private final ValidatorService validatorService = ExecutionContext.get(ValidatorService.class);

    public ApiResponse write(ApiRequest request) {
        var requests = nullToEmptyList(request.getRequests());
        if (requestWeight(requests) > MAX_BATCH_SIZE) {
            throw new ApiException(ErrorCode.TOO_MANY_REQUESTS_IN_BATCH, MAX_BATCH_SIZE);
        }
        List<RequestItemObject> items = createWriteObjects(requests);

        items.forEach(RequestItemObject::acknowledgeInCaches);

        validatorService.validate(request);
        items.forEach(RequestItemObject::validateBeforeTransaction);

        stateService.addStateFromRequest(request.getExistingNodes().getState());
        var existingNodesRequests = nullToEmptyMap(request.getExistingNodes().getRequests());

        var results = transactionManager.executeInTransaction(() -> {
            var responseItems = items.stream()
                    .peek(writeObject -> writeObject.resolveReferences(items, existingNodesRequests))
                    .map(RequestItemObject::write)
                    .collect(Collectors.toList());
            items.forEach(RequestItemObject::beforeCommit);
            return responseItems;
        });
        items.forEach(RequestItemObject::afterCommit);

        return new ApiResponse(results);
    }

    private int requestWeight(List<ApiRequestItem> requestItems) {
        return requestItems.stream()
                .mapToInt(value -> value instanceof UnlinkRequest ? 2 : 1)
                .sum();
    }

    private List<RequestItemObject> createWriteObjects(List<ApiRequestItem> requestItems) {
        List<RequestItemObject> writeObjects = new ArrayList<>();
        for (int i = 0; i < requestItems.size(); i++) {
            var requestItem = requestItems.get(i);
            var writeStrategyFactory = new RequestItemObjectFactory(new PathPrefix("requests", i));
            var writeStrategy = requestItem.createWriteObject(writeStrategyFactory);
            writeObjects.add(writeStrategy);
        }
        return unmodifiableList(writeObjects);
    }

    public NodeResponse read(ReadNodeRequest query) {
        return transactionManager.executeInReadTransaction(() -> readInTransaction(query));
    }

    private NodeResponse readInTransaction(ReadNodeRequest request) {
        NodeEntity entry = nodeRepository.readById(request.getId());

        if (entry == null) {
            throw new ApiException(ErrorCode.NODE_NOT_FOUND);
        }
        var response = MAPPER.convertValue(entry, NodeResponse.class);
        var showState = request.getShowState();
        if (isNotBlank(entry.getStateSha3()) && showState.isAppendState()) {
            try {
                verifyReadStatePermission(request, entry);

                var state = stateService.getStateMap(entry);
                if (entry.getStateSha3() != null && state.isEmpty() && showState.isFailOnNotAuthorized()) {
                    throw new ApiException(ErrorCode.STATE_DELETED);
                }

                response.setState(state.orElse(null));
            } catch (ApiException e) {
                if (showState.isFailOnNotAuthorized() || e.getErrorCode() != ErrorCode.PERMISSION_DENIED) {
                    throw e;
                }
            }
        }

        return response;
    }

    private void verifyReadStatePermission(ReadNodeRequest request, NodeEntity node) {
        EvaluationContextBuilder contextBuilder = new EvaluationContextBuilder()
                .withNodeObject(node);

        Authorize authorizeForAction = Authorize.forReadState(node.getId());
        Authorization authorization = authorizationService.processAuthorization(authorizeForAction, request.getAuthorizations(), contextBuilder);

        var permissionChecker = new PermissionChecker(RequestType.READ_STATE, node.getId(), authorization);

        try {
            PermissionCheckerContext.executeWithContext(permissionChecker, (unused) -> {
                stateService.validateStateAccess(node, currentUserService.getCurrentUser());
            });
        } finally {
            auditorNotifier.notify(node, permissionChecker.buildPermissionAudit());
        }
    }

}
