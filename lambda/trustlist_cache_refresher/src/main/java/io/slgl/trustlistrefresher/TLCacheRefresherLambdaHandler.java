package io.slgl.trustlistrefresher;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import io.slgl.api.ExecutionContext;
import io.slgl.api.document.service.TrustListManagementService;
import io.slgl.trustlistrefresher.config.TLCacheRefresherHandlerModule;

import static io.slgl.api.utils.TimerUtils.runWithTimer;

public class TLCacheRefresherLambdaHandler implements RequestHandler<ScheduledEvent, String> {

    static {
        ExecutionContext.requireModule(TLCacheRefresherHandlerModule.class);
    }

    @Override
    public String handleRequest(ScheduledEvent input, Context context) {
        var trustListManagementService = ExecutionContext.get(TrustListManagementService.class);
        runWithTimer("reload cache from s3", () -> trustListManagementService.reloadCacheFromS3(false));
        runWithTimer("online refresh", trustListManagementService::refresh);
        runWithTimer("export to s3", trustListManagementService::exportCacheToS3IfUpdated);

        return "200 OK";
    }
}
