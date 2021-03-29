package io.slgl.api;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.Map;

public class CorsHandler {

    public Map<String, String> getResponseHeaders(APIGatewayProxyRequestEvent request) {

        String httpMethod = request.getHttpMethod();
        String path = request.getPath();

        if ("/link".equals(path)) {
            if ("OPTIONS".equals(httpMethod)) {
                return ImmutableMap.of(
                        "Access-Control-Allow-Origin", "*",
                        "Access-Control-Allow-Methods", "POST, GET, OPTIONS",
                        "Access-Control-Allow-Headers", "Content-Type");
            } else {
                return ImmutableMap.of(
                        "Access-Control-Allow-Origin", "*");
            }
        }

        return Collections.emptyMap();
    }
}
