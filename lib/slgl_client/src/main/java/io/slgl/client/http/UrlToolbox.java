package io.slgl.client.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import io.slgl.client.error.SlglClientException;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.emptyMap;

class UrlToolbox {

    private final ObjectMapper objectMapper;

    public UrlToolbox(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    URI buildUrl(String baseUrl, String path) {
        return buildUrl(baseUrl, path, null);
    }

    URI buildUrl(String baseUrl, String path, Object queryParams) {
        try {
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            URIBuilder builder = new URIBuilder(baseUrl);
            String basePath = Objects.toString(builder.getPath(), "");
            builder.setPath(joinPath(basePath, path));

            convertToQueryMap(queryParams).forEach((name, value) -> {
                if (value instanceof List) {
                    List valueList = (List) value;
                    for (Object valueItem : valueList) {
                        builder.addParameter(name.toString(), valueItem.toString());
                    }
                } else {
                    builder.addParameter(name.toString(), value.toString());
                }
            });

            return builder.build();

        } catch (URISyntaxException e) {
            throw new SlglClientException(e);
        }
    }

    private Map<?, ?> convertToQueryMap(Object queryParams) {
        if (queryParams == null) {
            return emptyMap();
        }

        if (queryParams instanceof Map) {
            return ((Map<?, ?>) queryParams);
        }
        MapType mapType = objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class);
        return objectMapper.convertValue(queryParams, mapType);
    }

    private String joinPath(String first, String second) {
        if (!first.endsWith("/") && !second.startsWith("/")) {
            // add missing slash
            return first + '/' + second;
        }
        if (first.endsWith("/") && second.startsWith("/")) {
            // remove redundant slash
            return first + second.substring(1);
        }
        return first + second;
    }

}
