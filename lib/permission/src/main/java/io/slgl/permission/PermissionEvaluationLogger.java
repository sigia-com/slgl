package io.slgl.permission;

import io.slgl.client.audit.EvaluationLogEntry;
import io.slgl.permission.utils.TemporaryContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class PermissionEvaluationLogger {
    private CurrentLogEntries current;

    public CurrentLogEntries startLogging() {
        CurrentLogEntries previous = this.current;
        this.current = new CurrentLogEntries(previous);
        return this.current;
    }

    public void logDynamic(String code, Supplier<String> stringSupplier) {
        current.logDynamic(code, stringSupplier);
    }

    public void log(String code, CharSequence message) {
        current.log(code, message);
    }

    public class CurrentLogEntries implements TemporaryContext {

        private final CurrentLogEntries previous;
        private final List<EvaluationLogEntry> entries = new ArrayList<>();

        public CurrentLogEntries(CurrentLogEntries previous) {
            this.previous = previous;
        }

        public List<EvaluationLogEntry> get() {
            return entries;
        }

        @Override
        public void close() {
            if (current == this) {
                current = previous;
            }
        }

        private void logDynamic(String code, Supplier<String> stringSupplier) {
            log(code, new DynamicString(stringSupplier));
        }

        private void log(String code, CharSequence message) {
            requireNonNull(code);
            entries.add(new EvaluationLogEntry(code, message == null ? "" : message));
        }

    }

    private static class DynamicString implements CharSequence {
        private final Supplier<String> supplier;

        public DynamicString(Supplier<String> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
        }

        @Override
        public String toString() {
            return supplier.get();
        }

        @Override
        public int length() {
            return toString().length();
        }

        @Override
        public char charAt(int index) {
            return toString().charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return toString().subSequence(start, end);
        }
    }
}
