package io.slgl.client;

import io.slgl.client.http.HttpClientRequestExecutor;
import io.slgl.client.http.HttpRequestExecutor;
import io.slgl.client.http.LoggingHttpRequestExecutor;
import io.slgl.client.http.RestClient;
import io.slgl.client.node.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SlglApiClient implements AutoCloseable {

    private final RestClient restClient;
    private final List<SlglRequestListener> requestListeners;
    private final List<SlglResponseListener> responseListeners;

    private SlglApiClient(Builder builder) {
        restClient = new RestClient(builder.apiUrl, builder.username, builder.apiKey,
                builder.httpRequestExecutor != null ? builder.httpRequestExecutor : createDefaultHttpRequestExecutor());

        requestListeners = new ArrayList<>(builder.requestListeners);
        responseListeners = new ArrayList<>(builder.responseListeners);
    }

    public WriteResponse write(WriteRequest request) {
        return processResponseListeners(restClient.post("/", processRequestListeners(request), WriteResponse.class));
    }

    public WriteResponse write(WriteRequest.Builder requestBuilder) {
        return write(requestBuilder.build());
    }

    public WriteResponse write(String nodeDefinition) {
        return processResponseListeners(restClient.post("/", nodeDefinition, WriteResponse.class));
    }

    private WriteRequest processRequestListeners(WriteRequest request) {
        for (SlglRequestListener listener : requestListeners) {
            request = listener.onWriteRequest(request);
        }

        return request;
    }

    private WriteResponse processResponseListeners(WriteResponse response) {
        for (SlglResponseListener listener : responseListeners) {
            listener.onWriteResponse(response);
        }

        return response;
    }

    public NodeResponse writeNode(NodeRequest nodeRequest) {
        WriteRequest request = WriteRequest.builder()
                .addRequest(nodeRequest)
                .build();

        WriteResponse response = write(request);

        return ((NodeResponse) response.getResponses().get(0));
    }

    public NodeResponse writeNode(NodeRequest.Builder<?> node) {
        return writeNode(node.build());
    }

    public NodeResponse readNode(String id) {
        return readNode(ReadNodeRequest.builder().id(id).build());
    }

    public NodeResponse readNode(String id, ReadState showState) {
        return readNode(ReadNodeRequest.builder().id(id).showState(showState).build());
    }

    public NodeResponse readNode(ReadNodeRequest.Builder request) {
        return readNode(request.build());
    }

    public NodeResponse readNode(ReadNodeRequest request) {
        return restClient.get("/", request, NodeResponse.class);
    }

    @Override
    public void close() throws Exception {
        restClient.close();
    }

    public static HttpRequestExecutor createDefaultHttpRequestExecutor() {
        CloseableHttpClient httpClient = HttpClients.custom()
                .evictExpiredConnections()
                .evictIdleConnections(30, TimeUnit.SECONDS)
                .build();

        return new LoggingHttpRequestExecutor(
                new HttpClientRequestExecutor(httpClient),
                SlglApiClient.class
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String apiUrl;
        private String username;
        private String apiKey;

        private HttpRequestExecutor httpRequestExecutor;

        private final List<SlglRequestListener> requestListeners = new ArrayList<>();
        private final List<SlglResponseListener> responseListeners = new ArrayList<>();


        public Builder apiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder httpRequestExecutor(HttpRequestExecutor httpRequestExecutor) {
            this.httpRequestExecutor = httpRequestExecutor;
            return this;
        }

        public Builder requestListener(SlglRequestListener requestListener) {
            requestListeners.add(requestListener);
            return this;
        }

        public Builder requestListeners(List<? extends SlglRequestListener> requestListeners) {
            if (requestListeners != null) {
                this.requestListeners.addAll(requestListeners);
            }
            return this;
        }

        public Builder responseListener(SlglResponseListener responseListener) {
            responseListeners.add(responseListener);
            return this;
        }

        public Builder responseListeners(List<? extends SlglResponseListener> responseListeners) {
            if (responseListeners != null) {
                this.responseListeners.addAll(responseListeners);
            }
            return this;
        }

        public SlglApiClient build() {
            return new SlglApiClient(this);
        }
    }
}
