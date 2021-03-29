package io.slgl.api.service.handlers;

import io.slgl.api.ExecutionContext;
import io.slgl.api.domain.Link;
import io.slgl.api.protocol.LinkRequest;
import io.slgl.api.service.StateService;
import io.slgl.api.service.UserStateRepository;
import io.slgl.api.type.BuiltinType;
import io.slgl.api.type.TypeFactory;

import java.math.BigInteger;

import static com.google.common.base.Objects.equal;
import static io.slgl.api.domain.Anchors.CREDITS_ANCHOR;

public class UserCreditWriteHandler implements LinkWriteHandler {

    private final TypeFactory typeFactory = ExecutionContext.get(TypeFactory.class);
    private final StateService stateService = ExecutionContext.get(StateService.class);
    private final UserStateRepository userStateRepository = ExecutionContext.get(UserStateRepository.class);

    @Override
    public boolean isInterested(LinkRequest request) {
        return equal(request.getTargetAnchor(), CREDITS_ANCHOR);
    }

    @Override
    public void afterCommit(Link link, LinkRequest request) {
        if (isLinkedToUser(request)) {
            BigInteger credits = stateService.getStateProperty(link.getLinkSource(), "credits_amount", BigInteger.class)
                    .orElse(null);

            if (credits != null) {
                userStateRepository.addCredits(request.getTargetNode(), credits);
            }
        }
    }

    private boolean isLinkedToUser(LinkRequest request) {
        return typeFactory.getPublicTypeOfNode(request.getTargetNode()).isOrExtendsType(BuiltinType.USER);
    }
}
