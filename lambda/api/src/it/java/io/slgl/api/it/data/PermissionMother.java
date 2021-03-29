package io.slgl.api.it.data;

import io.slgl.client.node.permission.Permission;

import static io.slgl.client.node.permission.Allow.*;
import static io.slgl.client.node.permission.Requirement.requireThat;

public class PermissionMother {
    public static Permission readStateRequiringUsername(String username) {
        return Permission.builder()
                .allow(allowReadState())
                .requireAll(
                        requireThat("$principals").atLeastOne().meetsAllRequirements(
                                requireThat("$current.api.username").isEqualTo().value(username)
                        )
                )
                .build();
    }

    public static Permission allowAllForEveryone() {
        return Permission.builder()
                .allow(allowAll())
                .alwaysAllowed()
                .build();
    }

    public static Permission allowReadStateForEveryone() {
        return Permission.builder()
                .allow(allowReadState())
                .alwaysAllowed()
                .build();
    }

    public static Permission allowLinkForEveryone(String anchor) {
        return Permission.builder()
                .allow(allowLink(anchor))
                .alwaysAllowed()
                .build();
    }

    public static Permission allowUnlinkForEveryone(String anchor) {
        return Permission.builder()
                .allow(allowUnlink(anchor))
                .alwaysAllowed()
                .build();
    }
}
