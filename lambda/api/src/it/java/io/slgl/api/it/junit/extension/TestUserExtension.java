package io.slgl.api.it.junit.extension;

import io.slgl.api.it.user.User;
import org.junit.jupiter.api.extension.*;

import static com.google.common.base.Preconditions.checkNotNull;

public class TestUserExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private static final ThreadLocal<TestUserExtension> THREAD_LOCAL = new ThreadLocal<>();

    private User testUser;
    private User secondTestUser;
    private User thirdTestUser;

    public User getSharedTestUser() {
        if (testUser == null) {
            testUser = User.createUser();
            testUser.addCredits(1_000_000_000_000_000L);
        }

        return testUser;
    }

    public User getSharedSecondTestUser() {
        if (secondTestUser == null) {
            secondTestUser = User.createUser();
            secondTestUser.addCredits(1_000_000_000_000_000L);
        }

        return secondTestUser;
    }

    public User getSharedThirdTestUser() {
        if (thirdTestUser == null) {
            thirdTestUser = User.createUser();
            thirdTestUser.addCredits(1_000_000_000_000_000L);
        }

        return thirdTestUser;
    }

    public static TestUserExtension get() {
        return checkNotNull(THREAD_LOCAL.get(), "TestUserRule must be applied to test");
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        return type.isInstance(this);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return this;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        THREAD_LOCAL.set(this);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        THREAD_LOCAL.remove();
    }
}
