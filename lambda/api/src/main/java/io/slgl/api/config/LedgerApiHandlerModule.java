package io.slgl.api.config;

import io.slgl.api.ExecutionContext;
import io.slgl.api.ExecutionContextModule;
import io.slgl.api.controller.LedgerController;
import io.slgl.api.document.service.RefreshingScheduler;
import io.slgl.api.document.service.TrustListManagementService;
import io.slgl.api.service.CreditBillingService;
import io.slgl.api.service.CurrentUserSetter;

public class LedgerApiHandlerModule implements ExecutionContextModule {
    @Override
    public void configure() {
        ExecutionContext.requireModule(LedgerModule.class);

        ExecutionContext.require(
                CurrentUserSetter.class,
                LedgerController.class,
                CreditBillingService.class
        );

        ExecutionContext.put(TrustListManagementService.class, TrustListManagementService.offline());
        ExecutionContext.require(RefreshingScheduler.class);
    }
}
