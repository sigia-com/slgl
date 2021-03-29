package io.slgl.api.it.user;

import io.slgl.client.node.LinkResponse;
import io.slgl.client.node.NodeResponse;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SecretKey {
    private String secretKey;

    private NodeResponse response;
    private LinkResponse linkResponse;
}
