package io.slgl.api.it.assertj;

import io.slgl.client.audit.EvaluationLogEntry;
import io.slgl.client.audit.PermissionEvaluationResult;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Condition;

import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

@SuppressWarnings("UnusedReturnValue")
public class AuditDataPermissionResultAssert extends AbstractObjectAssert<AuditDataPermissionResultAssert, PermissionEvaluationResult> {

    public AuditDataPermissionResultAssert(PermissionEvaluationResult actual) {
        super(actual, AuditDataPermissionResultAssert.class);
    }

    public AuditDataPermissionResultAssert evaluationContextSatisfies(Consumer<Map<String, Object>> mapConsumer) {
        extracting(PermissionEvaluationResult::getEvaluationContext)
                .satisfies(mapConsumer);
        return this;
    }

    public AuditDataPermissionResultAssert hasPermissionContextValueAtPath(Object value, String... path) {
        evaluationContextSatisfies(ctx -> {
            Object current = ctx;
            for (String variable : path) {
                assertThat(current).as("parent of %s should be a Map", variable).isInstanceOf(Map.class);

                @SuppressWarnings("unchecked")
                var currentMap = (Map<Object, Object>) current;

                assertThat(currentMap).containsKey(variable);
                current = currentMap.get(variable);
            }
            assertThat(current).isEqualTo(value);
        });
        return this;
    }

    public AuditDataPermissionResultAssert doesNotHavePermissionContextValueAtPath(String... path) {
        evaluationContextSatisfies(ctx -> {
            Object current = ctx;
            for (String variable : path) {
                assertThat(current).as("parent of %s should be a Map", variable).isInstanceOf(Map.class);

                @SuppressWarnings("unchecked")
                var currentMap = (Map<Object, Object>) current;
                current = currentMap.get(variable);
            }
            assertThat(current).isNull();
        });
        return this;
    }

    public AuditDataPermissionResultAssert hasEvaluatedVariable(String variable, Object value) {
        extracting(PermissionEvaluationResult::getEvaluationLog, list(EvaluationLogEntry.class))
                .anySatisfy(entry -> {
                    assertThat(entry.getCode()).isEqualTo("evaluated_variable");
                    assertThat(entry.getMessage()).isEqualTo(String.format("`%s` -> %s", variable, value));
                });
        return this;
    }

    public AuditDataPermissionResultAssert hasEvaluationLogEntryWithCode(String entryCode) {
        extracting(PermissionEvaluationResult::getEvaluationLog, list(EvaluationLogEntry.class))
                .extracting(EvaluationLogEntry::getCode)
                .contains(entryCode);
        return this;
    }

    public AuditDataPermissionResultAssert isSuccess() {
        return is(Conditions.success());
    }

    public AuditDataPermissionResultAssert isFailed() {
        return isNot(Conditions.success());
    }

    private interface Conditions {


        static Condition<PermissionEvaluationResult> success() {
            return new Condition<>(PermissionEvaluationResult::isSuccess, "is success");
        }
    }

}
