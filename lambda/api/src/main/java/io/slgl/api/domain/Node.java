package io.slgl.api.domain;

import com.google.common.collect.ImmutableList;
import io.slgl.api.ExecutionContext;
import io.slgl.api.authorization.Authorization;
import io.slgl.api.authorization.AuthorizationService;
import io.slgl.api.authorization.model.Authorize;
import io.slgl.api.context.EvaluationContextBuilder;
import io.slgl.api.error.ApiException;
import io.slgl.api.permission.PermissionChecker;
import io.slgl.api.permission.PermissionCheckerContext;
import io.slgl.api.permission.service.AuditorNotifier;
import io.slgl.api.protocol.NodeRequest;
import io.slgl.api.protocol.NodeResponse;
import io.slgl.api.repository.NodeEntity;
import io.slgl.api.repository.NodeRepository;
import io.slgl.api.repository.TransactionManager;
import io.slgl.api.service.CurrentUserService;
import io.slgl.api.service.StateService;
import io.slgl.api.service.handlers.WriteHandlerService;
import io.slgl.api.type.BuiltinType;
import io.slgl.api.type.Type;
import io.slgl.api.type.TypeFactory;
import io.slgl.api.utils.ErrorCode;
import io.slgl.api.utils.PathPrefix;
import io.slgl.api.utils.json.InstantSerializer;
import io.slgl.client.audit.RequestType;
import io.slgl.permission.context.EvaluationContext;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.slgl.api.utils.Utils.generateId;
import static io.slgl.api.utils.Utils.getSha3OnJqSCompliantJson;
import static io.slgl.api.utils.json.UncheckedObjectMapper.MAPPER;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

@Slf4j
public class Node implements RequestItemObject {

    private final NodeRepository nodeRepository = ExecutionContext.get(NodeRepository.class);
    private final StateService stateService = ExecutionContext.get(StateService.class);
    private final TypeFactory typeFactory = ExecutionContext.get(TypeFactory.class);
    private final CurrentUserService currentUserService = ExecutionContext.get(CurrentUserService.class);
    private final WriteHandlerService writeHandlerService = ExecutionContext.get(WriteHandlerService.class);
    private final AuditorNotifier auditorNotifier = ExecutionContext.get(AuditorNotifier.class);
    private final AuthorizationService authorizationFactory = ExecutionContext.get(AuthorizationService.class);
    private final TransactionManager transactionManager = ExecutionContext.get(TransactionManager.class);

    private final NodeRequest request;
    private final UploadedFile uploadedFile;

    private final PathPrefix validationPathPrefix;

    private Type requestType;

    private Map<String, Object> state;
    private NodeEntity nodeEntity;
    private NodeResponse response;

    private String created = InstantSerializer.serialize(Instant.now());

    public Node(NodeRequest request, PathPrefix validationPathPrefix) {
        this.request = request;
        this.validationPathPrefix = validationPathPrefix;

        if (request.getFile() != null) {
            uploadedFile = new UploadedFile(request.getFile());
        } else {
            uploadedFile = null;
        }
    }

    @Override
    public void validateBeforeTransaction() {
        if (uploadedFile != null) {
            if (getRequestType().hasTemplates()) {
                NodeRequest requestFromFile = uploadedFile.getRequestObject();

                request.setData(requestFromFile.getData());
                request.setInlineLinks(requestFromFile.getInlineLinks());
                request.setRawJson(requestFromFile.getRawJson());
            } else {
                request.setRawJson(null);
            }
        }

        if (request.getId() == null) {
            request.setId(generateId());
        }

        getRequestType().validate(request, validationPathPrefix);

        if (uploadedFile != null) {
            getRequestType().validateFile(uploadedFile);
        }

        writeHandlerService.validate(request);

    }

    @Override
    public void acknowledgeInCaches() {
        if (getRequestType().isOrExtendsType(BuiltinType.TYPE)) {
            typeFactory.create(request); // put type in cache so that other request in batch can use it for validation
        }
    }


    @Override
    public NodeResponse write() {
        requireNonNull(transactionManager.getCurrentTransaction());

        verifyDuplicatedId();

        verifyPermissions();

        saveState();
        saveNodeEntity();

        return getResponse();
    }

    @Override
    public void beforeCommit() {
        writeHandlerService.beforeCommit(this);
    }

    @Override
    public void afterCommit() {
        writeHandlerService.afterCommit(this);
    }

    private void saveState() {
        Map<String, Object> state = getState();
        if (!state.isEmpty()) {
            stateService.saveState(request.getId(), state);
        }
    }

    private void saveNodeEntity() {
        NodeEntity node = getNodeEntity();
        nodeRepository.write(node);
    }

    private void verifyDuplicatedId() {
        if (BuiltinType.isBuiltinTypeId(request.getId())) {
            throw new ApiException(ErrorCode.NODE_ALREADY_EXISTS);
        }

        NodeEntity existing = nodeRepository.readById(request.getId());
        if (existing != null) {
            throw new ApiException(ErrorCode.NODE_ALREADY_EXISTS);
        }
    }

    private void verifyPermissions() {
        EvaluationContextBuilder contextBuilder = new EvaluationContextBuilder()
                .withRequestObject(request)
                .withNodeObject(getNodeEntity())
                .withUploadedFile(uploadedFile)
                .withCreated(created);

        Authorize authorizeForAction = null;
        Authorization authorization = authorizationFactory.processAuthorization(authorizeForAction, request.getAuthorizations(), contextBuilder);

        PermissionChecker permissionChecker = new PermissionChecker(RequestType.WRITE_NODE, request.getId(), authorization);

        try {
            PermissionCheckerContext.executeWithContext(permissionChecker, this::verifyInlineLinksPermissions);
        } finally {
            auditorNotifier.notify(getNodeEntity(), permissionChecker.buildPermissionAudit());
        }
    }

    private void verifyInlineLinksPermissions(PermissionChecker permissionChecker) {
        String nodeId = ofNullable(request.getId()).orElse("");

        verifyInlineLinksPermissions(permissionChecker, getRequestType(), request, emptyList(), nodeId);
    }

    private void verifyInlineLinksPermissions(
            PermissionChecker permissionChecker,
            Type type,
            NodeRequest object,
            List<NodeRequest> parents,
            String nodeId) {

        List<NodeRequest> newParents = ImmutableList.<NodeRequest>builder().addAll(parents).add(object).build();

        object.getInlineLinks().forEach((anchorId, inlineLinks) -> {
            inlineLinks.forEach((inlineLink) -> {
                EvaluationContext context = getInlineLinkContext(inlineLink, newParents);

                permissionChecker.verifyLinkPermission(type, context, nodeId, anchorId, true);

                verifyInlineLinksPermissions(
                        permissionChecker,
                        typeFactory.get(inlineLink, type.getAnchor(anchorId)),
                        inlineLink,
                        newParents,
                        nodeId.concat(anchorId)
                );
            });
        });
    }

    private EvaluationContext getInlineLinkContext(NodeRequest inlineLink, List<NodeRequest> newParents) {
        EvaluationContextBuilder contextBuilder = new EvaluationContextBuilder()
                .withInlineSourceNode(inlineLink)
                .withInlineParents(newParents)
                .withCreated(created)
                .withAuthorization(PermissionCheckerContext.get().getAuthorization());

        return contextBuilder.build();
    }

    private Type getRequestType() {
        if (requestType == null) {
            requestType = typeFactory.get(request);
        }

        return requestType;
    }

    private boolean hasCamouflage() {
        return request.getCamouflage() != null;
    }

    private String getCamouflagedTypeIfRequired() {
        return hasCamouflage() ? BuiltinType.CAMOUFLAGE.getId() : request.getType();
    }

    private NodeEntity getNodeEntity() {
        if (nodeEntity == null) {
            Map<String, Object> state = getState();

            String objectSha3 = request.getRawJson() != null ? getSha3OnJqSCompliantJson(request.getRawJson()) : null;
            String fileSha3 = uploadedFile != null ? uploadedFile.buildFileSha3() : null;
            String stateSha3 = !state.isEmpty() ? getSha3OnJqSCompliantJson(MAPPER.writeValueAsString(state)) : null;
            String camouflageSha3 = state.containsKey("@camouflage") ? getSha3OnJqSCompliantJson(MAPPER.writeValueAsString(state.get("@camouflage"))) : null;
            String stateSource = !state.isEmpty() ? request.getStateSource() : null;

            nodeEntity = new NodeEntity()
                    .setId(request.getId())
                    .setType(getCamouflagedTypeIfRequired())
                    .setStateSource(stateSource)
                    .setCreated(created)
                    .setObjectSha3(objectSha3)
                    .setFileSha3(fileSha3)
                    .setStateSha3(stateSha3)
                    .setStateSha3(stateSha3)
                    .setCamouflageSha3(camouflageSha3);

            if (request.getInlineType() != null && request.getInlineType().getExtendsType() != null) {
                nodeEntity.setExtendsType(request.getInlineType().getExtendsType());
            }
        }

        return nodeEntity;
    }

    private Map<String, Object> getState() {
        if (state == null) {
            state = stateService.buildStateMap(request, getRequestType());

            if (getRequestType().isOrExtendsType(BuiltinType.AUTHORIZATION)) {
                EvaluationContextBuilder contextBuilder = new EvaluationContextBuilder()
                        .withUploadedFile(uploadedFile);

                List<Object> principals = contextBuilder.getPrincipals().stream()
                        .map(EvaluationContext::deepInitialize)
                        .collect(Collectors.toList());

                state.put("authorization_principals", principals);
            }

            if (uploadedFile != null) {
                state.put("@file", uploadedFile.asEvaluationContext().asInitializedMap());
            }
        }

        return state;
    }

    public NodeResponse getResponse() {
        if (response == null) {
            NodeEntity nodeEntity = getNodeEntity();

            response = NodeResponse.fromNodeEntity(nodeEntity)
                    .setState(getState());
        }

        return response;
    }

    private ApiUser getCurrentUser() {
        return currentUserService.getCurrentUser();
    }

    public String getId() {
        return request.getId();
    }

    public NodeRequest getRequest() {
        return request;
    }

    public UploadedFile getUploadedFile() {
        return uploadedFile;
    }
}
