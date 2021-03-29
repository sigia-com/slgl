package io.slgl.api.it.junit.extension;

import org.junit.jupiter.api.extension.*;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkState;

public class AuditSQSExtension implements AfterTestExecutionCallback, ParameterResolver {
    private static final Set<String> injectedTestIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final AtomicInteger instanceCounter = new AtomicInteger(0);
    private static volatile AuditSQS messageCollection;

    @Override
    public synchronized void afterTestExecution(ExtensionContext context) {
        if (injectedTestIds.remove(context.getUniqueId())) {
            stop();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context) {
        return AuditSQS.class.equals(parameterContext.getParameter().getType());
    }

    @Override
    public synchronized AuditSQS resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        AuditSQS auditSqs = startIfNeeded();
        if (injectedTestIds.add(extensionContext.getUniqueId())) {
            instanceCounter.incrementAndGet();
        }
        return auditSqs;
    }

    private void stop() {
        int instanceCount = instanceCounter.decrementAndGet();
        if (instanceCount == 0) {
            messageCollection.close();
            messageCollection = null;
        }
    }

    public AuditSQS startIfNeeded() {
        if (instanceCounter.get() > 0) {
            checkState(messageCollection != null, "should be already started if instance count is > 0");
        } else {
            checkState(messageCollection == null, "shouldn't be already started if instance count is 0");
            messageCollection = new AuditSQS();
        }
        return messageCollection;
    }

}
