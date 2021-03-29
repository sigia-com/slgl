package io.slgl.api.controller;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import io.slgl.api.ExecutionContext;
import io.slgl.api.error.ApiException;
import io.slgl.api.protocol.*;
import io.slgl.api.service.LedgerService;
import io.slgl.api.utils.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;
import static io.slgl.api.utils.json.UncheckedObjectMapper.MAPPER;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
public class LedgerController {

    private LedgerService ledgerService = ExecutionContext.get(LedgerService.class);

    public NodeResponse read(APIGatewayProxyRequestEvent apiRequest) {
        Map<String, String> queryStringParameters = firstNonNull(apiRequest.getQueryStringParameters(), emptyMap());

        var id = queryStringParameters.get("id");
        if (StringUtils.isBlank(id)) {
            throw new ApiException(ErrorCode.EMPTY_QUERY_NOT_SUPPORTED);
        }

        ReadNodeRequest request = new ReadNodeRequest()
                .setId(id)
                .setShowState(getShowState(queryStringParameters.get("show_state")))
                .setAuthorizations(apiRequest.getMultiValueQueryStringParameters().getOrDefault("authorizations", Collections.emptyList()));

        return ledgerService.read(request);
    }

    public ApiResponse write(APIGatewayProxyRequestEvent request) {
        var body = getBody(request);
        ApiRequest apiRequest = MAPPER.readValue(body, ApiRequest.class);

        return ledgerService.write(apiRequest);
    }

    private String getBody(APIGatewayProxyRequestEvent request) {
        if (request.getBody() == null) {
            return "";
        }

        if (Boolean.TRUE.equals(request.getIsBase64Encoded())) {
            return new String(Base64.getDecoder().decode(request.getBody()));
        } else {
            return request.getBody();
        }
    }

    private ShowState getShowState(String showStateString) {
        if (isBlank(showStateString)) {
            return ShowState.DO_NOT_SHOW;
        }
        return ShowState.forValue(showStateString);
    }
}
