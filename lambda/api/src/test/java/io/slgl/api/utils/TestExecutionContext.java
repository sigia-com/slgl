package io.slgl.api.utils;

import io.slgl.api.ExecutionContext;
import org.mockito.Mockito;

public abstract class TestExecutionContext {
    public static <T> T mock(Class<T> clazz) {
        T mocked = Mockito.mock(clazz);
        ExecutionContext.put(clazz, mocked);
        return mocked;
    }
}
