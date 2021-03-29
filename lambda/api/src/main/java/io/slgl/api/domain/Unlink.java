package io.slgl.api.domain;

import io.slgl.api.ExecutionContext;
import io.slgl.api.authorization.Authorization;
import io.slgl.api.authorization.AuthorizationService;
import io.slgl.api.authorization.model.Authorize;
import io.slgl.api.camouflage.model.CamouflageData;
import io.slgl.api.context.EvaluationContextBuilder;
import io.slgl.api.error.ApiException;
import io.slgl.api.permission.PermissionChecker;
import io.slgl.api.permission.PermissionCheckerContext;
import io.slgl.api.permission.service.AuditorNotifier;
import io.slgl.api.protocol.UnlinkRequest;
import io.slgl.api.protocol.UnlinkResponse;
import io.slgl.api.repository.*;
import io.slgl.api.service.StateService;
import io.slgl.api.service.handlers.WriteHandlerService;
import io.slgl.api.type.Type;
import io.slgl.api.type.TypeFactory;
import io.slgl.api.utils.ErrorCode;
import io.slgl.client.audit.RequestType;
import io.slgl.permission.context.EvaluationContext;

import static io.slgl.api.camouflage.service.CamouflageHelper.extractCamouflageData;
import static io.slgl.api.camouflage.service.CamouflageHelper.isNodeCamouflaged;
import static java.util.Objects.requireNonNull;

public class Unlink implements RequestItemObject {

    private final TransactionManager transactionManager = ExecutionContext.get(TransactionManager.class);
    private final NodeRepository nodeRepository = ExecutionContext.get(NodeRepository.class);
    private final LinkRepository linkRepository = ExecutionContext.get(LinkRepository.class);
    private final TypeFactory typeFactory = ExecutionContext.get(TypeFactory.class);
    private final AuditorNotifier auditorNotifier = ExecutionContext.get(AuditorNotifier.class);
    private final StateService stateService = ExecutionContext.get(StateService.class);
    private final AuthorizationService authorizationFactory = ExecutionContext.get(AuthorizationService.class);
    private final WriteHandlerService writeHandlerService = ExecutionContext.get(WriteHandlerService.class);

    private final UnlinkRequest request;

    private LinkEntity link;
    private NodeEntity linkSource;
    private NodeEntity linkTarget;
    private Type linkTargetType;
    private String targetAnchor;
    private CamouflageData linkTargetCamouflage;

    public Unlink(UnlinkRequest request) {
        this.request = request;
    }

    @Override
    public UnlinkResponse write() {
        requireNonNull(transactionManager.getCurrentTransaction());
        verifyPermissions();

        var link = getLink();
        var response = new UnlinkResponse()
                .setId(link.getId())
                .setSourceNode(link.getSourceNode())
                .setTargetNode(link.getTargetNode())
                .setTargetAnchor(getTargetAnchor());
        linkRepository.delete(link);
        return response;
    }

    private void verifyPermissions() {
        EvaluationContextBuilder contextBuilder = new EvaluationContextBuilder()
                .withSourceNode(getLinkSource())
                .withTargetNode(getLinkTarget())
                .withCreated(getLink().getCreated());

        Authorize authorizeForAction = Authorize.forUnlinkFromAnchor(getLink().getTargetNode(), getTargetAnchor());
        Authorization authorization = authorizationFactory.processAuthorization(
                authorizeForAction, request.getAuthorizations(), contextBuilder);

        PermissionChecker permissionChecker = new PermissionChecker(
                RequestType.LINK_NODE, getLink().getTargetNode(), getTargetAnchor(), authorization);

        try {
            PermissionCheckerContext.executeWithContext(permissionChecker, this::verifyLinkPermissions);
        } finally {
            auditorNotifier.notify(getLinkTarget(), permissionChecker.buildPermissionAudit());
        }
    }

    private void verifyLinkPermissions(PermissionChecker permissionChecker) {
        EvaluationContext context = new EvaluationContextBuilder()
                .withSourceNode(getLinkSource())
                .withTargetNode(getLinkTarget())
                .withCreated(getLink().getCreated())
                .withAuthorization(permissionChecker.getAuthorization())
                .build();

        permissionChecker.verifyUnlinkPermission(
                getLinkTargetType(), context, getLink().getTargetNode(), getTargetAnchor(), false);
    }

    @Override
    public void beforeCommit() {
        writeHandlerService.beforeCommit(this);
    }

    @Override
    public void afterCommit() {
        writeHandlerService.afterCommit(this);
    }

    public LinkEntity getLink() {
        if (link == null) {
            link = linkRepository.readById(request.getId());
            if (link == null) {
                throw new ApiException(ErrorCode.LINK_NOT_FOUND, request.getId());
            }
        }

        return link;
    }

    public NodeEntity getLinkSource() {
        if (linkSource == null) {
            linkSource = nodeRepository.readById(getLink().getSourceNode());
            if (linkSource == null) {
                throw new ApiException(ErrorCode.LINKING_SOURCE_NODE_NOT_FOUND, getLink().getTargetNode());
            }
        }

        return linkSource;
    }

    public NodeEntity getLinkTarget() {
        if (linkTarget == null) {
            linkTarget = nodeRepository.readById(getLink().getTargetNode());
            if (linkTarget == null) {
                throw new ApiException(ErrorCode.LINKING_TARGET_NODE_NOT_FOUND, getLink().getTargetNode());
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

    public String getTargetAnchor() {
        if (targetAnchor == null) {
            targetAnchor = getLink().getTargetAnchor();

            if (isLinkTargetCamouflaged() && getLinkTargetCamouflage().isFakeAnchor(targetAnchor)) {
                targetAnchor = "#fake";
            }
        }

        return targetAnchor;
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
}
