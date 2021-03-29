package io.slgl.api;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import io.slgl.api.config.Provider;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.util.*;

import static io.slgl.api.utils.FunctionUtils.forEachEnsureEachCalled;
import static java.util.Collections.newSetFromMap;
import static java.util.Comparator.comparingInt;

@Slf4j
public class ExecutionContext {

    private static final ClassToInstanceMap<Object> instanceMap = MutableClassToInstanceMap.create();
    private static final Map<Class<?>, Provider<?>> providers = new HashMap<>();

    private static final ExecutionContextCallbacks callbacks = new ExecutionContextCallbacks();
    private static final Set<Class<? extends ExecutionContextModule>> loadedModules = new HashSet<>();

    public static void requireModule(Class<? extends ExecutionContextModule> module) {
        var alreadyLoaded = !loadedModules.add(module);
        if (alreadyLoaded) {
            log.debug("Module already loaded, skipping configuration: {}", module);
            return;
        }

        Stopwatch stopwatch = Stopwatch.createStarted();
        log.info("Configuring module: {}", module.getName());
        try {
            createInstance(module).configure();
        } finally {
            log.info("Module of type {} configured; time={}", module.getName(), stopwatch);
        }
    }

    public static <T> T get(Class<T> dependencyClass) {
        if (instanceMap.containsKey(dependencyClass)) {
            return instanceMap.getInstance(dependencyClass);

        } else {
            T instance = createInstance(dependencyClass);
            put(dependencyClass, instance);

            return instance;
        }
    }

    public static void require(Class<?>... types) {
        for (Class<?> clazz : types) {
            get(clazz);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T createInstance(Class<T> dependencyClass) {
        Provider<?> provider = providers.get(dependencyClass);
        if (provider != null) {
            return (T) provider.get();
        }

        try {
            Constructor<?> constructor = getTargetConstructor(dependencyClass);
            var params = Arrays.stream(constructor.getParameterTypes()).map(ExecutionContext::get).toArray();
            return (T) constructor.newInstance(params);

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> Constructor<?> getTargetConstructor(Class<T> dependencyClass) {
        return Arrays.stream(dependencyClass.getDeclaredConstructors())
                .min(comparingInt(it -> it.getParameters().length))
                .orElseThrow(() -> new IllegalArgumentException("Cannot create object without constructors: " + dependencyClass.getSimpleName()));
    }

    public static <T> Provider<T> getProvider(Class<T> dependencyClass) {
        return () -> get(dependencyClass);
    }

    public static <T> void put(Class<T> dependencyClass, T instance) {
        var previousInstance = instanceMap.putInstance(dependencyClass, instance);
        if (previousInstance != null) {
            throw new IllegalStateException("Instance of type has been already defined in ExecutionContext: " + previousInstance);
        }
        callbacks.registerIfEligible(instance);
    }

    public static void reset() {
        instanceMap.clear();
        callbacks.reset();
    }

    public static <T> void putProvider(Class<T> dependencyClass, Provider<T> provider) {
        if (instanceMap.containsKey(dependencyClass)) {
            throw new IllegalStateException("Instance for type has been already been created");
        }
        Provider<?> previousProvider = providers.put(dependencyClass, provider);
        if (previousProvider != null) {
            throw new IllegalStateException("Provider for type has been already defined in ExecutionContext: " + previousProvider);
        }
    }

    public static void beforeExecution() {
        callbacks.beforeExecution();
    }

    public static void afterExecution() {
        callbacks.afterExecution();
    }

    public static void onRequest(APIGatewayProxyRequestEvent request) {
        callbacks.onRequest(request);
    }

    public static void onResponse(APIGatewayProxyRequestEvent request, APIGatewayProxyResponseEvent response) {
        callbacks.onResponse(request, response);
    }

    private static class ExecutionContextCallbacks {

        private final Set<PostExecutionCallback> postExecutionCallbacks = newSetFromMap(new IdentityHashMap<>());
        private final Set<PreExecutionCallback> preExecutionCallbacks = newSetFromMap(new IdentityHashMap<>());
        private final Set<OnRequestCallback> onRequestCallbacks = newSetFromMap(new IdentityHashMap<>());
        private final Set<OnResponseCallback> onResponseCallbacks = newSetFromMap(new IdentityHashMap<>());

        @SuppressWarnings("SuspiciousMethodCalls")
        private void deregister(Object callback) {
            preExecutionCallbacks.remove(callback);
            onRequestCallbacks.remove(callback);
            onResponseCallbacks.remove(callback);
            postExecutionCallbacks.remove(callback);
        }

        private void registerIfEligible(Object instance) {
            if (instance instanceof OnResponseCallback) {
                onResponseCallbacks.add((OnResponseCallback) instance);
            }
            if (instance instanceof OnRequestCallback) {
                onRequestCallbacks.add((OnRequestCallback) instance);
            }
            if (instance instanceof PreExecutionCallback) {
                preExecutionCallbacks.add((PreExecutionCallback) instance);
            }
            if (instance instanceof PostExecutionCallback) {
                postExecutionCallbacks.add((PostExecutionCallback) instance);
            }
        }

        private void beforeExecution() {
            forEachEnsureEachCalled(callbacks.preExecutionCallbacks, PreExecutionCallback::beforeExecution);
        }

        private void afterExecution() {
            forEachEnsureEachCalled(callbacks.postExecutionCallbacks, PostExecutionCallback::afterExecution);
        }

        private void onRequest(APIGatewayProxyRequestEvent request) {
            forEachEnsureEachCalled(callbacks.onRequestCallbacks, aware -> aware.onRequest(request));
        }

        private void onResponse(APIGatewayProxyRequestEvent request, APIGatewayProxyResponseEvent response) {
            forEachEnsureEachCalled(callbacks.onResponseCallbacks, aware -> aware.onResponse(request, response));
        }

        public void reset() {
            preExecutionCallbacks.clear();
            onRequestCallbacks.clear();
            onResponseCallbacks.clear();
            postExecutionCallbacks.clear();
        }
    }

    public interface PreExecutionCallback {
        void beforeExecution();
    }

    public interface PostExecutionCallback {
        void afterExecution();
    }

    public interface OnRequestCallback {
        void onRequest(APIGatewayProxyRequestEvent request);
    }

    public interface OnResponseCallback {
        void onResponse(APIGatewayProxyRequestEvent request, APIGatewayProxyResponseEvent response);
    }
}
