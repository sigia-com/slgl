package io.slgl.api.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Objects;
import io.slgl.api.ExecutionContext;
import io.slgl.api.authorization.Authorization;
import io.slgl.api.authorization.AuthorizationService;
import io.slgl.api.authorization.model.Authorize;
import io.slgl.api.camouflage.model.CamouflageData;
import io.slgl.api.context.EvaluationContextBuilder;
import io.slgl.api.error.ApiException;
import io.slgl.api.observer.model.ObserverData;
import io.slgl.api.observer.model.ObserverEntity;
import io.slgl.api.observer.model.Result;
import io.slgl.api.observer.service.StorageUploaderFactory;
import io.slgl.api.permission.PermissionChecker;
import io.slgl.api.permission.PermissionCheckerContext;
import io.slgl.api.permission.service.AuditorNotifier;
import io.slgl.api.protocol.*;
import io.slgl.api.repository.*;
import io.slgl.api.service.CurrentUserService;
import io.slgl.api.service.LinksGetter;
import io.slgl.api.service.StateService;
import io.slgl.api.service.handlers.WriteHandlerService;
import io.slgl.api.type.Anchor;
import io.slgl.api.type.Type;
import io.slgl.api.type.TypeFactory;
import io.slgl.api.utils.ErrorCode;
import io.slgl.api.utils.json.InstantSerializer;
import io.slgl.client.audit.RequestType;
import io.slgl.permission.context.EvaluationContext;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.base.Objects.equal;
import static io.slgl.api.camouflage.service.CamouflageHelper.extractCamouflageData;
import static io.slgl.api.camouflage.service.CamouflageHelper.isNodeCamouflaged;
import static io.slgl.api.permission.model.EvaluationLogCodes.ANCHOR_MAX_SIZE_EXCEEDED;
import static io.slgl.api.utils.Utils.getSha3OnJqSCompliantJson;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

public class Link implements RequestItemObject {

    private static final TypeReference<List<?>> LIST_TYPE = new TypeReference<>() {
    };

    private final LinkRepository linkRepository = ExecutionContext.get(LinkRepository.class);
    private final TransactionManager transactionManager = ExecutionContext.get(TransactionManager.class);
    private final NodeRepository nodeRepository = ExecutionContext.get(NodeRepository.class);
    private final TypeFactory typeFactory = ExecutionContext.get(TypeFactory.class);
    private final AuthorizationService authorizationFactory = ExecutionContext.get(AuthorizationService.class);
    private final AuditorNotifier auditorNotifier = ExecutionContext.get(AuditorNotifier.class);
    private final StateService stateService = ExecutionContext.get(StateService.class);
    private final WriteHandlerService writeHandlerService = ExecutionContext.get(WriteHandlerService.class);
    private final LinksGetter linksGetter = ExecutionContext.get(LinksGetter.class);
    private final StorageUploaderFactory storageUploaderFactory = ExecutionContext.get(StorageUploaderFactory.class);
    private final CurrentUserService currentUserService = ExecutionContext.get(CurrentUserService.class);

    private final LinkRequest request;

    private NodeEntity linkSource;
    private Type linkSourceType;

    private NodeEntity linkTarget;
    private Type linkTargetType;

    private String targetAnchor;
    private Anchor linkTargetAnchor;

    private CamouflageData linkTargetCamouflage;

    private List<ObserverEntity> observers;
    private ObserverData observerData;

    private String created = InstantSerializer.serialize(Instant.now());

    public Link(LinkRequest request) {
        this.request = request;
    }

    @Override
    public void resolveReferences(List<RequestItemObject> writtenInCurrentRequest, Map<String, NodeRequest> existingNodesRequests) {
        if (request.getSourceNodeRef() != null && request.getSourceNode() == null) {
            var resolved = resolveNodeReference(writtenInCurrentRequest, request.getSourceNodeRef());
            request.setSourceNode(resolved.getId());
        }

        if (request.getTargetNodeRef() != null && request.getTargetNode() == null) {
            var resolved = resolveNodeReference(writtenInCurrentRequest, request.getTargetNodeRef());
            request.setTargetNode(resolved.getId());
        }

        if (!getObservers().isEmpty()) {
            observerData = new ObserverData()
                    .setSourceNode(request.getSourceNode())
                    .setTargetNode(request.getTargetNode())
                    .setTargetAnchor(request.getTargetAnchor());

            String observedNodeId = request.getSourceNode();

            Node observedNode = writtenInCurrentRequest.stream()
                    .flatMap(it -> it instanceof Node ? Stream.of(((Node) it)) : Stream.of())
                    .filter(node -> equal(node.getId(), observedNodeId))
                    .findFirst().orElse(null);
            NodeRequest observedRequest = existingNodesRequests.get(observedNodeId);

            if (observedNode != null) {
                observerData.setRawJson(observedNode.getRequest().getRawJson())
                        .setNode(observedNode.getResponse())
                        .setFile(observedNode.getUploadedFile());

            } else if (observedRequest != null){
                NodeEntity observedNodeEntity = getLinkSource();

                NodeResponse observedResponse = NodeResponse.fromNodeEntity(observedNodeEntity);
                if (observedNodeEntity.getStateSha3() != null) {
                    observedResponse.setState(stateService.getStateMap(observedNodeEntity).orElse(null));
                }
                observerData.setNode(observedResponse);

                UploadedFile observedFile = observedRequest.getFile() != null ? new UploadedFile(observedRequest.getFile()) : null;

                String rawJson;
                if (observedFile != null) {
                    if (!Objects.equal(observedNodeEntity.getFileSha3(), observedFile.buildFileSha3())) {
                        throw new ApiException(ErrorCode.FILE_HASH_MISMATCH, observedNodeId);
                    }
                    observerData.setFile(observedFile);

                    rawJson = observedFile.getRequestObject().getRawJson();

                } else {
                    rawJson = observedRequest.getRawJson();
                }

                String objectSha3 = rawJson != null ? getSha3OnJqSCompliantJson(rawJson) : null;
                if (!Objects.equal(observedNodeEntity.getObjectSha3(), objectSha3)) {
                    throw new ApiException(ErrorCode.OBJECT_HASH_MISMATCH, observedNodeId);
                }
                observerData.setRawJson(rawJson);

            } else {
                throw new ApiException(ErrorCode.LINKING_TO_NODE_WITH_OBSERVERS_USING_NODE_NOT_PROVIDED_IN_THIS_REQUEST);
            }
        }
    }

    private Node resolveNodeReference(List<RequestItemObject> writtenInCurrentRequest, int refIndex) {
        var item = writtenInCurrentRequest.get(refIndex);
        if (item instanceof Node) {
            return (Node) item;
        }
        throw new IllegalStateException("Cannot find referenced node (should be already validated)");
    }

    @Override
    public ApiResponseItem write() {
        requireNonNull(transactionManager.getCurrentTransaction());

        verifyDuplicatedLink();

        getLinkSourceType().validateCanLinkToAnchor(getLinkAnchor());
        verifyPermissions();

        LinkEntity linkEntity = saveLinkEntity();

        return new LinkResponse()
                .setId(linkEntity.getId())
                .setSourceNode(linkEntity.getSourceNode())
                .setTargetNode(linkEntity.getTargetNode())
                .setTargetAnchor(linkEntity.getTargetAnchor());
    }

    @Override
    public void beforeCommit() {
        notifyObservers();

        writeHandlerService.beforeCommit(this, request);
    }

    private void notifyObservers() {
        if (getObservers().isEmpty()) {
            return;
        }

        for (ObserverEntity observer : getObservers()) {
            if (observer.hasStorage()) {
                Result result = storageUploaderFactory
                        .createUploader(observer.getStorage())
                        .upload(observerData, observer.getPgpKey())
                        .log();
                if (result.isError() && nonNull(observer.getRecoveryStorage())) {
                    storageUploaderFactory
                            .createRecoveryUploader(observer.getRecoveryStorage().getPath())
                            .upload(observerData, observer.getPgpKey())
                            .log("Recovery upload");
                }
            }
        }
    }

    @Override
    public void afterCommit() {
        writeHandlerService.afterCommit(this, request);
    }

    private void verifyDuplicatedLink() {
        LinkEntity existing = linkRepository.read(request.getSourceNode(), request.getTargetNode(), getTargetAnchor());
        if (existing != null) {
            throw new ApiException(ErrorCode.LINK_ALREADY_EXISTS);
        }
    }

    private void verifyPermissions() {
        EvaluationContextBuilder contextBuilder = new EvaluationContextBuilder()
                .withSourceNode(getLinkSource())
                .withTargetNode(getLinkTarget())
                .withCreated(created);

        Authorize authorizeForAction = Authorize.forLinkToAnchor(request.getTargetNode(), getTargetAnchor());
        Authorization authorization = authorizationFactory.processAuthorization(
                authorizeForAction, request.getAuthorizations(), contextBuilder);

        PermissionChecker permissionChecker = new PermissionChecker(
                RequestType.LINK_NODE, request.getTargetNode(), getTargetAnchor(), authorization);

        try {
            PermissionCheckerContext.executeWithContext(permissionChecker, this::verifyLinkPermissions);
        } finally {
            auditorNotifier.notify(getLinkTarget(), permissionChecker.buildPermissionAudit());
        }
    }

    private void verifyLinkPermissions(PermissionChecker permissionChecker) {
        validateAnchorMaxSize(permissionChecker);

        EvaluationContext context = new EvaluationContextBuilder()
                .withSourceNode(getLinkSource())
                .withTargetNode(getLinkTarget())
                .withCreated(created)
                .withAuthorization(permissionChecker.getAuthorization())
                .build();

        permissionChecker.verifyLinkPermission(
                getLinkTargetType(), context, request.getTargetNode(), getTargetAnchor(), false);
    }

    private void validateAnchorMaxSize(PermissionChecker permissionChecker) {
        if (hasExceededMaxSize()) {
            permissionChecker.addEvaluationLogAndMarkAsFailed(ANCHOR_MAX_SIZE_EXCEEDED, "Unable to link more nodes than maximum size allowed by anchor definition.");
            throw new ApiException(ErrorCode.PERMISSION_DENIED);
        }
    }

    private boolean hasExceededMaxSize() {
        Anchor anchor = getLinkAnchor();
        if (anchor.getMaxSize() == null) {
            return false;
        }

        var inlineLinksCount = stateService.getStateProperty(getLinkTarget(), anchor.getId(), LIST_TYPE)
                .map(List::size)
                .orElse(0);
        if (inlineLinksCount >= anchor.getMaxSize()) {
            // fail fast to avoid redundant db hits
            return true;
        }

        int regularLinksCount = linkRepository
                .readAllByTargetNodeAndTargetAnchor(request.getTargetNode(), getTargetAnchor()).size();

        var allLinksCount = inlineLinksCount + regularLinksCount;
        return allLinksCount >= anchor.getMaxSize();
    }

    private LinkEntity saveLinkEntity() {
        LinkEntity linkEntity = new LinkEntity()
                .setSourceNode(request.getSourceNode())
                .setTargetNode(request.getTargetNode())
                .setTargetAnchor(getCamouflagedTargetAnchor().orElse(request.getTargetAnchor()))
                .setCreated(created);

        linkRepository.write(linkEntity);

        return linkEntity;
    }

    public NodeEntity getLinkSource() {
        if (linkSource == null) {
            linkSource = nodeRepository.readById(request.getSourceNode());
            if (linkSource == null) {
                throw new ApiException(ErrorCode.LINKING_SOURCE_NODE_NOT_FOUND, request.getTargetNode());
            }
        }

        return linkSource;
    }

    public Type getLinkSourceType() {
        if (linkSourceType == null) {
            linkSourceType = typeFactory.get(getLinkSource());
        }

        return linkSourceType;
    }

    public NodeEntity getLinkTarget() {
        if (linkTarget == null) {
            linkTarget = nodeRepository.readById(request.getTargetNode());
            if (linkTarget == null) {
                throw new ApiException(ErrorCode.LINKING_TARGET_NODE_NOT_FOUND, request.getTargetNode());
            }
        }

        return linkTarget;
    }

    public Type getLinkTargetType() {
        if (linkTargetType == null) {
            linkTargetType = typeFactory.get(getLinkTarget());
        }

        return linkTargetType;
    }

    private String getTargetAnchor() {
        if (targetAnchor == null) {
            targetAnchor = request.getTargetAnchor();

            if (isLinkTargetCamouflaged() && getLinkTargetCamouflage().isFakeAnchor(targetAnchor)) {
                targetAnchor = "#fake";
            }
        }

        return targetAnchor;
    }

    private Anchor getLinkAnchor() {
        if (linkTargetAnchor == null) {
            linkTargetAnchor = getLinkTargetType().getAnchor(getTargetAnchor());

            if (linkTargetAnchor == null) {
                if (isLinkTargetCamouflaged()) {
                    throw new ApiException(ErrorCode.PERMISSION_DENIED);
                }
                throw new ApiException(ErrorCode.LINKING_TARGET_ANCHOR_NOT_FOUND, getTargetAnchor());
            }
        }

        return linkTargetAnchor;
    }

    private boolean isLinkTargetCamouflaged() {
        return isNodeCamouflaged(getLinkTarget());
    }

    private CamouflageData getLinkTargetCamouflage() {
        if (isLinkTargetCamouflaged() && linkTargetCamouflage == null) {
            linkTargetCamouflage = extractCamouflageData(stateService.getStateMap(getLinkTarget())).orElse(null);
        }
        return linkTargetCamouflage;
    }

    private Optional<String> getCamouflagedTargetAnchor() {
        return isLinkTargetCamouflaged() && !"#fake".equals(getTargetAnchor())
                ? Optional.of(getLinkTargetCamouflage().getCamouflagedAnchor(getTargetAnchor()))
                : Optional.empty();
    }

    private List<ObserverEntity> getObservers() {
        if (observers == null) {
            observers = linksGetter.getObservers(getLinkTarget());
        }

        return observers;
    }
}
