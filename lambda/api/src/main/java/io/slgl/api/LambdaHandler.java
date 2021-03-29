package io.slgl.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import io.slgl.api.config.LedgerApiHandlerModule;
import io.slgl.api.error.ApiException;
import io.slgl.api.utils.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

import static io.slgl.api.utils.json.UncheckedObjectMapper.MAPPER;

@Slf4j
public class LambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    static {
        ExecutionContext.requireModule(LedgerApiHandlerModule.class);
    }

    private final Router router = ExecutionContext.get(Router.class);
    private final CorsHandler corsHandler = ExecutionContext.get(CorsHandler.class);
    private final ExceptionConverter exceptionConverter = new ExceptionConverter();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            authorize(request);
        } catch (Exception e) {
            return handleException(request, e);
        }
        ExecutionContext.beforeExecution();
        try {
            var response = innerHandleRequest(request);
            return onResponse(request, response);
        } finally {
            ExecutionContext.afterExecution();
        }
    }

    private APIGatewayProxyResponseEvent innerHandleRequest(APIGatewayProxyRequestEvent request) {
        try {
            ExecutionContext.onRequest(request);
            var responseBody = router.route(request);
            return toApiResponse(request, responseBody);

        } catch (Throwable e) {
            return handleException(request, e);
        }
    }

    private APIGatewayProxyResponseEvent onResponse(APIGatewayProxyRequestEvent request, APIGatewayProxyResponseEvent response) {
        try {
            ExecutionContext.onResponse(request, response);
            return response;
        } catch (Exception e) {
            log.error("Unexpected exception in ExecutionContext#onResponse");
            return handleException(request, e);
        }
    }

    private void authorize(APIGatewayProxyRequestEvent request) {
        Optional.of(request)
                .map(APIGatewayProxyRequestEvent::getRequestContext)
                .map(APIGatewayProxyRequestEvent.ProxyRequestContext::getAuthorizer)
                .map(el -> el.get("credits"))
                .map(Objects::toString)
                .map(str -> MAPPER.readValue(str, BigInteger.class))
                .ifPresent(credits -> {
                    if (credits.signum() != 1) {
                        throw new ApiException(ErrorCode.NO_API_CREDITS_LEFT);
                    }
                });
    }

    public APIGatewayProxyResponseEvent handleException(APIGatewayProxyRequestEvent request, Throwable e) {
        APIGatewayProxyResponseEvent response;
        ApiException apiException = exceptionConverter.convertToApiException(e);
        if (apiException.getErrorCode() == ErrorCode.UNKNOWN_ERROR) {
            log.error("Unknown exception when handling request", e);
        }
        response = toApiResponse(request, apiException);
        return response;
    }

    private APIGatewayProxyResponseEvent toApiResponse(APIGatewayProxyRequestEvent request, Object response) {
        APIGatewayProxyResponseEvent apiResponse = new APIGatewayProxyResponseEvent();
        apiResponse.setStatusCode(response != null ? HttpStatus.SC_OK : HttpStatus.SC_NO_CONTENT);
        apiResponse.setBody(response != null ? MAPPER.writeValueAsString(response) : null);
        apiResponse.setHeaders(corsHandler.getResponseHeaders(request));
        return apiResponse;
    }

    private APIGatewayProxyResponseEvent toApiResponse(APIGatewayProxyRequestEvent request, ApiException e) {
        APIGatewayProxyResponseEvent apiResponse = new APIGatewayProxyResponseEvent();
        apiResponse.setStatusCode(e.getErrorCode().getHttpStatus());
        apiResponse.setBody(MAPPER.writeValueAsString(e.createErrorResponse()));
        apiResponse.setHeaders(corsHandler.getResponseHeaders(request));
        return apiResponse;
    }

}
