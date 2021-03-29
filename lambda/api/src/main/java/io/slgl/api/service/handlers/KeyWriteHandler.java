package io.slgl.api.service.handlers;

import io.slgl.api.ExecutionContext;
import io.slgl.api.domain.Link;
import io.slgl.api.protocol.LinkRequest;
import io.slgl.api.service.StateService;
import io.slgl.api.service.UserStateRepository;
import io.slgl.api.type.BuiltinType;
import io.slgl.api.type.TypeFactory;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkState;
import static io.slgl.api.domain.Anchors.KEYS_ANCHOR;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class KeyWriteHandler implements LinkWriteHandler {

    private final TypeFactory typeFactory = ExecutionContext.get(TypeFactory.class);
    private final UserStateRepository userStateRepository = ExecutionContext.get(UserStateRepository.class);
    private final StateService stateService = ExecutionContext.get(StateService.class);

    @Override
    public boolean isInterested(LinkRequest request) {
        return equal(request.getTargetAnchor(), KEYS_ANCHOR);
    }

    @Override
    public void afterCommit(Link link, LinkRequest request) {
        var targetType = typeFactory.getPublicTypeOfNode(request.getTargetNode());
        if (!targetType.isOrExtendsType(BuiltinType.USER)) {
            return;
        }

        var value = stateService.getStateProperty(link.getLinkSource(), "value", String.class).orElse(null);
        checkState(isNotBlank(value), "Key must not be blank");

        userStateRepository.addKey(request.getTargetNode(), request.getSourceNode(), value);
    }
}
