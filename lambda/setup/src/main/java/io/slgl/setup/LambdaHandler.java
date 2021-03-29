package io.slgl.setup;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.slgl.api.ExecutionContext;
import io.slgl.api.config.LedgerModule;
import io.slgl.api.utils.LambdaEnv;
import io.slgl.setup.CustomResourceResponseEvent.DataResponse;
import io.slgl.setup.actions.AdminUserSetup;
import io.slgl.setup.actions.QldbLinkIndexTableSetup;
import io.slgl.setup.actions.QldbLinkTableSetup;
import io.slgl.setup.actions.QldbNodeTableSetup;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static io.slgl.setup.CustomResourceResponseEvent.buildFailed;
import static io.slgl.setup.CustomResourceResponseEvent.buildSuccess;

@Slf4j
public class LambdaHandler implements RequestHandler<CustomResourceRequestEvent, CustomResourceResponseEvent> {

    @Override
    public CustomResourceResponseEvent handleRequest(CustomResourceRequestEvent request, Context context) {
        try {
            log.info("Request = {}", request);
            LambdaEnv.override(request.getResourceProperties().getEnvironmentVariables());
            ExecutionContext.requireModule(LedgerModule.class);
            ExecutionContext.beforeExecution();

            DataResponse responseData = new DataResponse();

            switch (request.getRequestType()) {
                case "Create":
                case "Update":
                    ExecutionContext.get(QldbNodeTableSetup.class).execute();
                    ExecutionContext.get(QldbLinkTableSetup.class).execute();
                    ExecutionContext.get(QldbLinkIndexTableSetup.class).execute();

                    ExecutionContext.get(AdminUserSetup.class).execute(request, responseData);

                case "Delete":
                    // do nothing
            }

            return sendResponse(request, buildSuccess(context, request, responseData));

        } catch (Exception e) {
            log.error("Unknown exception when handling request", e);
            return sendResponse(request, buildFailed(e, context, request));
        }
    }

    private CustomResourceResponseEvent sendResponse(CustomResourceRequestEvent request, CustomResourceResponseEvent response) {
        String responseJson = serializeToJson(response);

        log.info("Sending response to {}:\n{}", request.getResponseURL(), responseJson);

        HttpPut httpRequest = new HttpPut(request.getResponseURL());
        httpRequest.setEntity(new StringEntity(responseJson, ContentType.APPLICATION_JSON));

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            httpClient.execute(httpRequest);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return response;
    }

    private String serializeToJson(Object object) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.enable(INDENT_OUTPUT);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
