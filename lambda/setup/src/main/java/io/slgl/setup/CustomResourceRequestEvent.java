package io.slgl.setup;

import lombok.Data;

import java.util.Map;

@Data
public class CustomResourceRequestEvent {

    public String RequestType;

    public String ResponseURL;

    public String StackId;
    public String RequestId;
    public String LogicalResourceId;
    public String PhysicalResourceId;

    public ResourceProperties ResourceProperties;

    @Data
    public static class ResourceProperties {

        public String ledgerName;
        public Map<String, String> EnvironmentVariables;

        public String AdminApiKey;
    }
}
