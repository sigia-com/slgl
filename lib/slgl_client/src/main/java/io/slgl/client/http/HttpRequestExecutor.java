package io.slgl.client.http;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;

public interface HttpRequestExecutor extends AutoCloseable {

    HttpResponse execute(HttpUriRequest request) throws IOException;

    @Override
    default void close() throws Exception {
    }
}
