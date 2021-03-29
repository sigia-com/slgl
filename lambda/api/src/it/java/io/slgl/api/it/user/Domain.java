package io.slgl.api.it.user;

import io.slgl.api.it.data.TestDataUtils;
import io.slgl.client.node.NodeResponse;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Domain {
    private String value;
    private NodeResponse response;

    public String generateUniqueId(String s) {
        return TestDataUtils.generateUniqueId();
    }

    public String generateUniqueId() {
        return TestDataUtils.generateUniqueId();
    }
}
