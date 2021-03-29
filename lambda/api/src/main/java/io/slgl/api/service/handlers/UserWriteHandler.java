package io.slgl.api.service.handlers;

import io.slgl.api.ExecutionContext;
import io.slgl.api.domain.Node;
import io.slgl.api.protocol.NodeRequest;
import io.slgl.api.service.UserStateRepository;
import io.slgl.api.type.BuiltinType;
import io.slgl.api.type.TypeFactory;

public class UserWriteHandler implements NodeWriteHandler {

    private final UserStateRepository userStateRepository = ExecutionContext.get(UserStateRepository.class);
    private final TypeFactory typeFactory = ExecutionContext.get(TypeFactory.class);

    @Override
    public boolean isInterested(NodeRequest object) {
        return true;
    }

    @Override
    public void afterCommit(Node node) {
        NodeRequest request = node.getRequest();

        if (isUser(request)) {
            userStateRepository.createUserEntry(request.getId());
        }
    }

    private boolean isUser(NodeRequest object) {
        return typeFactory.get(object).isOrExtendsType(BuiltinType.USER);
    }
}
