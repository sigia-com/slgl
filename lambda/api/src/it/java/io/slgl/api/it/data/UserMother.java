package io.slgl.api.it.data;

import io.slgl.client.Types;
import io.slgl.client.node.NodeRequest;
import io.slgl.client.node.WriteRequest;
import org.apache.commons.lang3.RandomStringUtils;

import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;

public class UserMother {

    public static NodeRequest.Builder<?> createUserRequest() {
        return NodeRequest.builder()
                .id(generateUniqueId())
                .type(Types.USER);
    }

    public static WriteRequest createNewKeyRequest(String userId) {
        return createNewKeyRequest(userId, generateUniqueKey());
    }

    public static WriteRequest createNewKeyRequest(String userId, String key) {
        return WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(Types.KEY)
                        .data("value", key))
                .addLinkRequest(0, userId, "#keys")
                .build();
    }

    public static WriteRequest createDeleteKeyRequest(String keyLinkId) {
        return WriteRequest.builder()
                .addUnlinkRequest(keyLinkId)
                .build();
    }

    public static String generateUniqueKey() {
        return RandomStringUtils.randomAlphanumeric(10);
    }
}
