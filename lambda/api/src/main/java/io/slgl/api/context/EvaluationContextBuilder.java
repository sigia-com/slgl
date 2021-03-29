package io.slgl.api.context;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import io.slgl.api.ExecutionContext;
import io.slgl.api.authorization.Authorization;
import io.slgl.api.camouflage.model.CamouflageData;
import io.slgl.api.context.principal.DocumentSignaturePrincipal;
import io.slgl.api.context.principal.Principal;
import io.slgl.api.domain.ApiUser;
import io.slgl.api.domain.UploadedFile;
import io.slgl.api.protocol.NodeRequest;
import io.slgl.api.repository.LinkEntity;
import io.slgl.api.repository.LinkRepository;
import io.slgl.api.repository.NodeEntity;
import io.slgl.api.repository.NodeRepository;
import io.slgl.api.service.CurrentUserService;
import io.slgl.api.service.StateService;
import io.slgl.api.type.Anchor;
import io.slgl.api.type.Type;
import io.slgl.api.type.TypeFactory;
import io.slgl.permission.context.EvaluationContext;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Suppliers.memoize;
import static io.slgl.api.camouflage.service.CamouflageHelper.extractCamouflageData;
import static io.slgl.api.utils.json.UncheckedObjectMapper.MAPPER;

public class EvaluationContextBuilder {

    private static final Pattern PATH_WITH_NODE_ID = Pattern.compile("@\\('(?<id>[^)]*)'\\)");

    private final NodeRepository nodeRepository = ExecutionContext.get(NodeRepository.class);
    private final LinkRepository linkRepository = ExecutionContext.get(LinkRepository.class);
    private final StateService stateService = ExecutionContext.get(StateService.class);
    private final TypeFactory typeFactory = ExecutionContext.get(TypeFactory.class);
    private final CurrentUserService currentUserService = ExecutionContext.get(CurrentUserService.class);

    private NodeRequest nodeRequest;
    private UploadedFile uploadedFile;

    private NodeEntity nodeObject;

    private NodeRequest inlineSourceNode;
    private List<NodeRequest> inlineParents;

    private NodeEntity parent;
    private Type parentType;

    private NodeEntity sourceNode;
    private NodeEntity targetNode;

    private String created;

    private final List<Principal> principals = new ArrayList<>();
    private Authorization authorization;

    public EvaluationContextBuilder() {
        ApiUser currentUser = currentUserService.getCurrentUser();
        if (currentUser != null) {
            withPrincipal(currentUser.asPrincipal());
        }
    }

    public EvaluationContextBuilder withRequestObject(NodeRequest nodeRequest) {
        this.nodeRequest = nodeRequest;
        return this;
    }

    public EvaluationContextBuilder withUploadedFile(UploadedFile uploadedFile) {
        this.uploadedFile = uploadedFile;
        return this;
    }

    public EvaluationContextBuilder withNodeObject(NodeEntity nodeObject) {
        this.nodeObject = nodeObject;
        return this;
    }

    public EvaluationContextBuilder withInlineSourceNode(NodeRequest inlineSourceNode) {
        this.inlineSourceNode = inlineSourceNode;
        return this;
    }

    public EvaluationContextBuilder withInlineParents(List<NodeRequest> inlineParents) {
        this.inlineParents = ImmutableList.copyOf(inlineParents);
        return this;
    }

    public EvaluationContextBuilder withParent(NodeEntity parent, Type parentType) {
        this.parent = parent;
        this.parentType = parentType;
        return this;
    }

    public EvaluationContextBuilder withSourceNode(NodeEntity sourceNode) {
        this.sourceNode = sourceNode;
        return this;
    }

    public EvaluationContextBuilder withTargetNode(NodeEntity targetNode) {
        this.targetNode = targetNode;
        return this;
    }

    public EvaluationContextBuilder withCreated(String created) {
        this.created = created;
        return this;
    }

    public EvaluationContextBuilder withPrincipal(Principal principal) {
        if (principal != null) {
            principals.add(principal);
        }
        return this;
    }

    public EvaluationContextBuilder withAuthorization(Authorization authorization) {
        this.authorization = authorization;
        return this;
    }

    public EvaluationContext build() {
        var context = EvaluationContext.builder();

        if (nodeRequest != null) {
            Supplier<EvaluationContext> parentSupplier = null;
            if (parent != null) {
                parentSupplier = memoize(() -> createNodeContext(parent, parentType));
            }

            if (inlineParents != null) {
                for (NodeRequest inlineParent : inlineParents) {
                    Supplier<EvaluationContext> finalParentSupplier = parentSupplier;
                    parentSupplier = () -> createNodeContext(inlineParent, null, finalParentSupplier);
                }
            }

            Supplier<EvaluationContext> finalParentSupplier = parentSupplier;

            Supplier<EvaluationContext> nodeSupplier = memoize(() -> createNodeContext(nodeRequest, nodeObject, finalParentSupplier));
            context.provider("$node", nodeSupplier);
            context.provider("$parent", () -> nodeSupplier.get().get("$parent"));

        } else if (nodeObject != null) {
            Supplier<EvaluationContext> nodeSupplier = memoize(() -> createNodeContext(nodeObject));
            context.provider("$node", nodeSupplier);
            context.provider("$parent", () -> nodeSupplier.get().get("$parent"));
        }

        if (uploadedFile != null) {
            context.provider("$file", () -> uploadedFile.asEvaluationContext());
        }

        if (inlineSourceNode != null) {
            Supplier<EvaluationContext> parentSupplier = null;
            for (NodeRequest inlineParent : inlineParents) {
                Supplier<EvaluationContext> finalParentSupplier = parentSupplier;
                parentSupplier = () -> createNodeContext(inlineParent, null, finalParentSupplier);
            }

            Supplier<EvaluationContext> sourceNodeSupplier = memoize(() -> createNodeContext(inlineSourceNode, null, null));

            context.provider("$source_node", sourceNodeSupplier);
            context.provider("$target_node", parentSupplier);
        }

        if (sourceNode != null) {
            Supplier<EvaluationContext> sourceNodeSupplier = memoize(() -> createNodeContext(sourceNode));
            context.provider("$source_node", sourceNodeSupplier);
        }
        if (targetNode != null) {
            Supplier<EvaluationContext> targetNodeSupplier = memoize(() -> createNodeContext(targetNode));
            context.provider("$target_node", targetNodeSupplier);
        }

        if (created != null) {
            context.value("$created", created);
        }

        context.provider("$principals", memoize(this::getPrincipals));

        context.provider(PATH_WITH_NODE_ID, (matcher, key) -> createNodeContextForId(matcher.group("id")));

        return context.build();
    }

    private EvaluationContext createNodeContext(NodeRequest request, NodeEntity entry, Supplier<EvaluationContext> parentSupplier) {
        io.slgl.permission.context.EvaluationContextBuilder context = EvaluationContext.builder();

        context.values(request.toMap());

        if (entry != null) {
            context.values(MAPPER.convertValue(entry, Map.class));
        }

        // if (parentSupplier != null) {
        //     context.provider("$links", () -> ImmutableList.of(parentSupplier.get()));
        // }

        return context.build();
    }

    private EvaluationContext createNodeContext(NodeEntity object) {
        return createNodeContext(object, typeFactory.get(object));
    }

    private EvaluationContext createNodeContext(NodeEntity node, Type type) {
        io.slgl.permission.context.EvaluationContextBuilder context = EvaluationContext.builder()
                .values(MAPPER.convertValue(node, new TypeReference<>() {
                }));

        Map<String, Object> state = stateService.getStateMap(node).orElseGet(Collections::emptyMap);

        for (String stateProperty : type.getStateProperties()) {
            if (!stateProperty.startsWith("#")) {
                context.provider(stateProperty, () -> {
                    stateService.validateStateAccess(node, currentUserService.getPermissionsUser());
                    return state.get(stateProperty);
                });
            }
        }

        if (state.containsKey("@file")) {
            context.provider("$file", () -> {
                stateService.validateStateAccess(node, currentUserService.getPermissionsUser());
                return state.get("@file");
            });
        }

        Collection<Anchor> anchors = type.getAnchors();
        if (anchors != null) {
            for (Anchor anchor : type.getAnchors()) {
                context.provider(anchor.getId(), () -> createNodeAnchorContext(node, anchor, getInlineLinks(state, anchor)));
            }
        }

        // context.provider("$links", () -> createNodeLinksContext(node));

        return context.build();
    }

    private List<?> getInlineLinks(Map<String, Object> state, Anchor anchor) {
        Object inlineLinks = state.get(anchor.getId());

        if (inlineLinks instanceof List) {
            return (List<?>) inlineLinks;
        } else {
            return null;
        }
    }

//    private List<EvaluationContext> createNodeLinksContext(EntryEntity node) {
//        List<LinkEntity> links = linkRepository.readAllBySourceNode(node.getId());
//
//        return links.stream()
//                .sorted(Comparator.comparing(LinkEntity::getCreated))
//                .map(link -> createNodeContext(entryRepository.readById(link.getTargetNode())))
//                .collect(Collectors.toList());
//    }

    private EvaluationContext createNodeContextForId(String id) {
        NodeEntity parentObject = nodeRepository.readById(id);

        return createNodeContext(parentObject);
    }

    private List<EvaluationContext> createNodeAnchorContext(NodeEntity node, Anchor anchor, List<?> inlineLinks) {
        Supplier<String> realAnchor = memoize(() -> {
            Optional<CamouflageData> camouflageData = extractCamouflageData(stateService.getStateMap(node));
            return camouflageData.map(c -> c.getCamouflagedAnchor(anchor.getId())).orElse(anchor.getId());
        });

        Supplier<List<? extends EvaluationContext>> list = () -> {
            var links = linkRepository.readAllByTargetNodeAndTargetAnchor(node.getId(), realAnchor.get());

            List<EvaluationContext> anchorContext = links.stream()
                    .sorted(Comparator.comparing(LinkEntity::getCreated))
                    .map(link -> EvaluationContext.lazy(() -> createNodeContext(nodeRepository.readById(link.getSourceNode()))))
                    .collect(Collectors.toList());

            if (inlineLinks != null) {
                for (Object inlineLink : inlineLinks) {
                    anchorContext.add(EvaluationContext.lazy(() -> EvaluationContext.wrap(inlineLink)));
                }
            }
            return anchorContext;
        };

        Supplier<EvaluationContext> first = () -> {
            if (inlineLinks != null && !inlineLinks.isEmpty()) {
                return EvaluationContext.wrap(inlineLinks.get(0));
            }

            LinkEntity link = linkRepository.readFirstByTargetNodeAndTargetAnchor(node.getId(), realAnchor.get());
            if (link != null) {
                return createNodeContext(nodeRepository.readById(link.getSourceNode()));
            }

            return null;
        };

        Supplier<EvaluationContext> last = () -> {
            LinkEntity link = linkRepository.readLastByTargetNodeAndTargetAnchor(node.getId(), realAnchor.get());
            if (link != null) {
                return createNodeContext(nodeRepository.readById(link.getSourceNode()));
            }

            if (inlineLinks != null && !inlineLinks.isEmpty()) {
                return EvaluationContext.wrap(inlineLinks.get(inlineLinks.size() - 1));
            }

            return null;
        };

        return new AnchorsLazyList<>(list, first, last);
    }

    public List<EvaluationContext> getPrincipals() {
        List<EvaluationContext> principals = new ArrayList<>();

        for (Principal principal : this.principals) {
            principals.add(principal.asEvaluationContext());
        }

        if (uploadedFile != null) {
            for (DocumentSignaturePrincipal principal : uploadedFile.getCertificatePrincipals()) {
                principals.add(principal.asEvaluationContext());
            }
        }

        if (authorization != null) {
            for (Object principal : authorization.getAuthorizationPrincipals()) {
                principals.add(EvaluationContext.wrap(principal));
            }
        }

        return principals;
    }
}
