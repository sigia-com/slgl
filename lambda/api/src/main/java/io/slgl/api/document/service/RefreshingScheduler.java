package io.slgl.api.document.service;

import io.slgl.api.ExecutionContext;
import io.slgl.api.utils.LambdaEnv;

import java.time.Instant;

import static io.slgl.api.utils.TimerUtils.runWithTimer;
import static java.time.temporal.ChronoUnit.MINUTES;

public class RefreshingScheduler implements ExecutionContext.PreExecutionCallback {
    private final int refreshRateMinutes = LambdaEnv.DssCache.getOfflineReloadRateInMinutes();
    private final TrustListManagementService trustListManagementService = ExecutionContext.get(TrustListManagementService.class);
    private Instant lastRefresh;

    @Override
    public void beforeExecution() {
        var currentTime = Instant.now();
        if (lastRefresh == null || lastRefresh.until(currentTime, MINUTES) > refreshRateMinutes) {
            refresh();
            lastRefresh = currentTime;
        }
    }

    private void refresh() {
        runWithTimer("s3 cache reload: ", () -> trustListManagementService.reloadCacheFromS3(false));
        runWithTimer("TL refresh: ", trustListManagementService::refresh);
    }
}
