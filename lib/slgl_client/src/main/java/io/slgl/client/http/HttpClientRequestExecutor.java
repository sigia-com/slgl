package io.slgl.client.http;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.Closeable;
import java.io.IOException;

import static java.util.Objects.requireNonNull;

public class HttpClientRequestExecutor implements HttpRequestExecutor {

    private final HttpClient httpClient;

    public HttpClientRequestExecutor(HttpClient httpClient) {
        this.httpClient = requireNonNull(httpClient);
    }

    @Override
    public HttpResponse execute(HttpUriRequest request) throws IOException {
        return httpClient.execute(request);
    }

    @Override
    public void close() throws Exception {
        if (httpClient instanceof Closeable) {
            ((Closeable) httpClient).close();
        }
    }
}
