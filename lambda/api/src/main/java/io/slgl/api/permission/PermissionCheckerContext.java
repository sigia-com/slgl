package io.slgl.api.permission;

import io.slgl.client.utils.Preconditions;

import java.util.function.Consumer;

public class PermissionCheckerContext {

    private static PermissionChecker context;

    public static void executeWithContext(PermissionChecker permissionChecker, Consumer<PermissionChecker> process) {
        try {
            set(permissionChecker);

            process.accept(permissionChecker);

        } finally {
            reset();
        }
    }

    private static void set(PermissionChecker permissionChecker) {
        Preconditions.checkState(context == null, "Context already created");

        context = permissionChecker;
    }

    private static void reset() {
        Preconditions.checkState(context != null, "Context not created");

        context = null;
    }

    public static PermissionChecker get() {
        Preconditions.checkState(context != null, "Context not created");

        return context;
    }
}
