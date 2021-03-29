package io.slgl.api;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import io.slgl.api.controller.LedgerController;
import io.slgl.api.error.ApiException;
import io.slgl.api.utils.ErrorCode;

import java.util.regex.Pattern;

import static com.google.common.base.Objects.equal;

public class Router {

    private static final Pattern LINK_PATTERN = Pattern.compile("/links/([^/]+)");

    private final LedgerController ledgerController = ExecutionContext.get(LedgerController.class);

    public Object route(APIGatewayProxyRequestEvent request) {
        String httpMethod = request.getHttpMethod();
        String path = request.getPath();

        if (equal(path, "/")) {
            if (equal(httpMethod, "GET")) {
                return ledgerController.read(request);
            }
            if (equal(httpMethod, "POST")) {
                return ledgerController.write(request);
            }
            if (equal(httpMethod, "OPTIONS")) {
                return null;
            }

            throw new ApiException(ErrorCode.HTTP_METHOD_NOT_SUPPORTED);
        }

        throw new ApiException(ErrorCode.HTTP_PATH_NOT_FOUND);
    }
}
