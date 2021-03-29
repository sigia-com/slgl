package io.slgl.api.authorization.model;

import io.slgl.api.validator.ValidAnchor;
import io.slgl.api.validator.ValidNodeId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

@Setter
@Getter
@Accessors(chain = true)
@EqualsAndHashCode
public class Authorize {

    @NotNull
    private AuthorizeAction action;

    @NotNull
    @ValidNodeId
    private String node;

    @ValidAnchor
    private String anchor;

    public static Authorize forLinkToAnchor(String node, String anchor) {
        return new Authorize()
                .setAction(AuthorizeAction.LINK_TO_ANCHOR)
                .setNode(node)
                .setAnchor(anchor);
    }

    public static Authorize forUnlinkFromAnchor(String node, String anchor) {
        return new Authorize()
                .setAction(AuthorizeAction.UNLINK_FROM_ANCHOR)
                .setNode(node)
                .setAnchor(anchor);
    }

    public static Authorize forReadState(String node) {
        return new Authorize()
                .setAction(AuthorizeAction.READ_STATE)
                .setNode(node);
    }
}
