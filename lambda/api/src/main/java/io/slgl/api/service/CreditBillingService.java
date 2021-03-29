package io.slgl.api.service;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import io.slgl.api.ExecutionContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkState;

@Slf4j
public class CreditBillingService implements ExecutionContext.OnRequestCallback, ExecutionContext.OnResponseCallback {

    private final UserStateRepository userStateRepository = ExecutionContext.get(UserStateRepository.class);
    private final CurrentUserService currentUserService = ExecutionContext.get(CurrentUserService.class);

    private static final BigInteger creditsForLambdaCall = BigInteger.valueOf(20 * 10);
    private static final BigInteger timeFactor = BigInteger.valueOf(166);
    private static final BigInteger responseSizeFactor = BigInteger.valueOf(500);
    private static final BigInteger requestSizeFactor = BigInteger.valueOf(500 * 12 * 10);

    private BillingContext currentContext;

    @Override
    public void onRequest(APIGatewayProxyRequestEvent request) {
        this.currentContext = new BillingContext(request);
    }

    @Override
    public void onResponse(APIGatewayProxyRequestEvent request, APIGatewayProxyResponseEvent response) {
        var currentUser = currentUserService.getCurrentUser();
        if (currentUser == null) {
            log.info("no user context, credit charging skipped");
            return;
        }
        BigInteger credits = calculateCreditsToCharge(request, response);
        BigInteger newCredits = userStateRepository.chargeCredits(currentUser.getId(), credits);
        addCreditsToHeaders(response, credits, newCredits);
    }

    private void addCreditsToHeaders(APIGatewayProxyResponseEvent response, BigInteger credits, BigInteger newCredits) {
        var previousHeaders = response.getHeaders();
        @SuppressWarnings("UnstableApiUsage")
        var headers = ImmutableMap
                .<String, String>builderWithExpectedSize(previousHeaders.size() + 1)
                .putAll(previousHeaders)
                .put("SLGL-Used-Credits", String.valueOf(credits))
                .put("SLGL-Remaining-Credits", String.valueOf(newCredits))
                .build();
        response.setHeaders(headers);
    }

    private BigInteger calculateCreditsToCharge(APIGatewayProxyRequestEvent request, APIGatewayProxyResponseEvent response) {
        int requestSize = getBodySize(request);
        int responseSize = getBodySize(response);
        long time = roundUpTo(currentContext.getTimeMs(), 100);

        BigInteger credits = creditsForLambdaCall
                .add(timeFactor.multiply(BigInteger.valueOf(time)))
                .add(requestSizeFactor.multiply(BigInteger.valueOf(requestSize)))
                .add(responseSizeFactor.multiply(BigInteger.valueOf(responseSize)));
        log.info("Charging credits: {} for [base time: {}ms; request body size: {}; response body size: {}]", credits, time, requestSize, responseSize);
        return credits;
    }

    private int getBodySize(APIGatewayProxyRequestEvent request) {
        return request.getBody() == null ? 0 : request.getBody().length();
    }

    private int getBodySize(APIGatewayProxyResponseEvent response) {
        return response.getBody() == null ? 0 : response.getBody().length();
    }

    private long roundUpTo(long time, long base) {
        return (time % base == 0) ? time : base + (base * (time / base));
    }

    @AllArgsConstructor
    private class BillingContext {
        private final Stopwatch stopWatch = Stopwatch.createStarted();
        private final APIGatewayProxyRequestEvent request;

        private long getTimeMs() {
            requireCurrent();
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
            return stopWatch.elapsed(TimeUnit.MILLISECONDS);
        }

        private void requireCurrent() {
            checkState(currentContext.request == request);
        }
    }
}
