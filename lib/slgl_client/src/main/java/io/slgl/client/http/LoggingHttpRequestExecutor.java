package io.slgl.client.http;

import io.slgl.client.utils.IOUtils;
import io.slgl.client.utils.StopWatch;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BufferedHttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.MILLIS;

public class LoggingHttpRequestExecutor implements HttpRequestExecutor {

    private final HttpRequestExecutor delegate;
    private final Logger logger;

    public LoggingHttpRequestExecutor(HttpRequestExecutor delegate, Class<?> loggerClass) {
        this.delegate = delegate;
        logger = LoggerFactory.getLogger(loggerClass);
    }

    @Override
    public HttpResponse execute(HttpUriRequest request) throws IOException {
        StopWatch stopWatch = StopWatch.start();
        try {
            HttpResponse response = ensureRepeatableResponse(delegate.execute(request));
            log(request, response, stopWatch.getTime(MILLIS));
            return response;

        } catch (Exception e) {
            log(request, e, stopWatch.getTime(MILLIS));
            throw e;
        }
    }

    private static HttpResponse ensureRepeatableResponse(HttpResponse response) throws IOException {
        if (response.getEntity().isRepeatable()) {
            return response;
        }

        response.setEntity(new BufferedHttpEntity(response.getEntity()));
        return response;
    }

    private void log(HttpUriRequest request, HttpResponse response, long time) throws IOException {
        if (!logger.isInfoEnabled()) {
            return;
        }

        logger.info(formatRequest(request) + "\n\n" + formatResponse(response, time) + "\n");
    }

    private void log(HttpUriRequest request, Exception e, long time) throws IOException {
        if (!logger.isInfoEnabled()) {
            return;
        }

        logger.info(formatRequest(request) + "\n" + "Time: " + time + " ms", e);
    }

    private String formatRequest(HttpUriRequest request) throws IOException {
        String result = format("Request[method=%s, uri=%s]\n", request.getMethod(), request.getURI());
        result += formatHeaders(request.getAllHeaders());

        String requestBody = formatEntity(request);
        if (requestBody != null) {
            result += requestBody;
        }

        return result;
    }

    private String formatResponse(HttpResponse response, long time) throws IOException {
        String result = format("Response[status=%d %s, time=%s ms]\n", response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase(), time);
        result += formatHeaders(response.getAllHeaders());

        String responseBody = formatEntity(response.getEntity());
        if (responseBody != null) {
            result += responseBody;
        }

        return result;
    }

    private String formatHeaders(Header[] headers) {
        if (!logger.isDebugEnabled()) {
            return "";
        }

        String result = "";
        for (Header header : headers) {
            result += header.getName() + ": " + header.getValue() + "\n";
        }

        return result;
    }

    private String formatEntity(HttpUriRequest request) throws IOException {
        if (!(request instanceof HttpEntityEnclosingRequest)) {
            return null;
        }

        return formatEntity(((HttpEntityEnclosingRequest) request).getEntity());
    }

    private String formatEntity(HttpEntity entity) throws IOException {
        if (entity == null || !entity.isRepeatable()) {
            return "";
        }

        try (InputStream content = entity.getContent()) {
            return IOUtils.toString(content);
        }
    }
}
