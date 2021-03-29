package io.slgl.api.it.assertj;

import io.slgl.api.utils.json.UncheckedObjectMapper;
import io.slgl.client.audit.*;
import io.slgl.client.node.LinkResponse;
import io.slgl.client.protocol.Identified;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Condition;

import java.util.Collection;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.stream;

@SuppressWarnings("UnusedReturnValue")
public class AuditDataAssert extends AbstractObjectAssert<AuditDataAssert, PermissionAuditMessage> {

    public AuditDataAssert(PermissionAuditMessage actual) {
        super(actual, AuditDataAssert.class);
    }

    public AuditDataAssert hasRequestType(RequestType requestType) {
        return is(Conditions.hasRequestType(requestType));
    }

    public AuditDataAssert isSuccess() {
        return is(Conditions.success());
    }

    public AuditDataAssert isFailed() {
        return isNot(Conditions.success());
    }

    public AuditDataAssert hasNode(Identified node) {
        return hasNode(node.getId());
    }

    public AuditDataAssert hasNode(String nodeId) {
        assertThat(actual.getPermissionAudit().getNode()).isEqualTo(nodeId);
        return myself;
    }

    public AuditDataAssert hasAnchor(String anchor) {
        assertThat(actual.getPermissionAudit().getAnchor()).isEqualTo(anchor);
        return myself;
    }

    public AuditDataAssert hasNodeAndAnchorMatchingLink(LinkResponse link) {
        assertThat(actual.getPermissionAudit().getNode()).isEqualTo(link.getTargetNode());
        assertThat(actual.getPermissionAudit().getAnchor()).isEqualTo(link.getTargetAnchor());
        return myself;
    }

    public AuditDataAssert hasEvaluatedVariable(String variable, Object value) {
        extracting(PermissionAuditMessage::getPermissionAudit)
                .extracting(result -> Stream.concat(
                        result.getEvaluatedPermissions().stream()
                                .map(PermissionEvaluation::getEvaluationResults)
                                .flatMap(Collection::stream)
                                .map(PermissionEvaluationResult::getEvaluationLog)
                                .flatMap(Collection::stream),
                        result.getEvaluationLog().stream()
                ), stream(EvaluationLogEntry.class))
                .anySatisfy(entry -> {
                    assertThat(entry.getCode()).isEqualTo("variable_evaluation_result");
                    assertThat(entry.getMessage()).isEqualTo(String.format("%s\n-> %s", variable, UncheckedObjectMapper.MAPPER.writeValueAsString(value)));
                });
        return this;
    }

    public AuditDataAssert hasEvaluationLogEntryWithCode(String entryCode) {
        extracting(PermissionAuditMessage::getPermissionAudit)
                .extracting(PermissionAudit::getEvaluationLog, list(EvaluationLogEntry.class))
                .extracting(EvaluationLogEntry::getCode)
                .contains(entryCode);

        return this;
    }

    private interface Conditions {

        static Condition<PermissionAuditMessage> hasRequestType(RequestType requestType) {
            return new Condition<>(
                    data -> data.getPermissionAudit().getRequestType() == requestType,
                    "Request type is  %s", requestType
            );
        }

        static Condition<PermissionAuditMessage> success() {
            return new Condition<>(data -> data.getPermissionAudit().isSuccess(), "is success");
        }
    }

}
