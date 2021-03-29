package io.slgl.api.repository;

import io.slgl.api.ExecutionContext;
import io.slgl.api.utils.Reference;
import software.amazon.qldb.Executable;
import software.amazon.qldb.QldbDriver;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class TransactionManager {

    private final QldbDriver qldbDriver = ExecutionContext.get(QldbDriver.class);
    private final ThreadLocal<Executable> currentTransaction = new ThreadLocal<>();

    public void executeInTransaction(Consumer<Executable> work) {
        executeInTransaction(() -> work.accept(getCurrentTransaction()));
    }

    public <T> T executeInTransaction(Function<Executable, T> work) {
        return executeInTransaction(() -> work.apply(getCurrentTransaction()));
    }

    public void executeInTransaction(Runnable work) {
        executeInTransaction(() -> {
            work.run();
            return null;
        });
    }

    public <T> T executeInReadTransaction(Function<Executable, T> work) {
        return executeInReadTransaction(() -> work.apply(getCurrentTransaction()));
    }

    public <T> T executeInReadTransaction(Supplier<T> call) {
        Reference<T> result = new Reference<>();
        try {
            return executeInTransaction(() -> {
                result.set(call.get()); // plain, as volatile access is not needed on the same thread
                throw new ReadAccessCommitPreventingException();
            });
        } catch (ReadAccessCommitPreventingException ignored) {
            return result.get();
        }
    }

    public <T> T executeInTransaction(Supplier<T> call) {
        checkState(currentTransaction.get() == null, "Nested QLDB transaction are not supported");
        try {
            return qldbDriver.execute(tx -> {
                currentTransaction.set(tx);
                return call.get();
            });
        } finally {
            currentTransaction.remove();
        }
    }

    public Executable getCurrentTransaction() {
        return checkNotNull(currentTransaction.get(), "Transaction not present");
    }

    public <T> T ensureReadTransaction(Function<Executable, T> call) {
        Executable transaction = currentTransaction.get();
        if (transaction != null) {
            return call.apply(transaction);
        } else {
            return executeInReadTransaction(() -> call.apply(currentTransaction.get()));
        }
    }

    private static class ReadAccessCommitPreventingException extends RuntimeException {

        @Override
        public Throwable fillInStackTrace() {
            // we don't need costly stacktrace here
            return this;
        }
    }
}
