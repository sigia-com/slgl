package io.slgl.api.service;

import io.slgl.api.domain.Link;
import io.slgl.api.domain.Node;
import io.slgl.api.domain.Unlink;
import io.slgl.api.protocol.LinkRequest;
import io.slgl.api.protocol.NodeRequest;
import io.slgl.api.protocol.UnlinkRequest;
import io.slgl.api.utils.PathPrefix;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RequestItemObjectFactory {

    private final PathPrefix pathPrefix;

    public Node writeObjectFor(NodeRequest request) {
        return new Node(request, pathPrefix);
    }

    public Link writeObjectFor(LinkRequest link) {
        return new Link(link);
    }

    public Unlink writeObjectFor(UnlinkRequest unlink) {
        return new Unlink(unlink);
    }
}
