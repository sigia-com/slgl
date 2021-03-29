package io.slgl.api.service.handlers;

import io.slgl.api.ExecutionContext;
import io.slgl.api.domain.Unlink;
import io.slgl.api.service.UserStateRepository;
import io.slgl.api.type.BuiltinType;
import io.slgl.api.type.TypeFactory;

import static com.google.common.base.Objects.equal;
import static io.slgl.api.domain.Anchors.KEYS_ANCHOR;

public class KeyDeletionWriteHandler implements UnlinkWriteHandler {

    private final UserStateRepository userStateRepository = ExecutionContext.get(UserStateRepository.class);
    private final TypeFactory typeFactory = ExecutionContext.get(TypeFactory.class);

    @Override
    public boolean isInterested(Unlink unlink) {
        return equal(unlink.getLinkTargetType().getId(), BuiltinType.USER.getId())
                && equal(unlink.getTargetAnchor(), KEYS_ANCHOR);
    }

    @Override
    public void beforeCommit(Unlink unlink) {
        var userId = unlink.getLinkTarget().getId();
        var keyId = unlink.getLinkSource().getId();

        var keyType = typeFactory.getPublicTypeOfNode(keyId);
        if (!keyType.isOrExtendsType(BuiltinType.KEY)) {
            return;
        }

        userStateRepository.removeKey(userId, keyId);
    }
}
