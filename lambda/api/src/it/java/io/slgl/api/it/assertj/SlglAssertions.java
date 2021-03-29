package io.slgl.api.it.assertj;

import io.slgl.client.audit.PermissionAuditMessage;
import io.slgl.client.audit.PermissionEvaluationResult;
import io.slgl.client.error.ErrorResponse;
import io.slgl.client.error.SlglApiException;
import io.slgl.client.node.NodeResponse;
import io.slgl.client.node.WriteResponse;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.ThrowableAssert;

public class SlglAssertions extends Assertions {

    public static ErrorAssert assertThat(ErrorResponse actual) {
        return new ErrorAssert(actual);
    }

    public static WriteResponseAssert assertThat(WriteResponse actual) {
        return new WriteResponseAssert(actual);
    }

    public static NodeResponseAssert assertThat(NodeResponse actual) {
        return new NodeResponseAssert(actual);
    }

    public static AuditDataAssert assertThat(PermissionAuditMessage actual) {
        return new AuditDataAssert(actual);
    }

    public static AuditDataPermissionResultAssert assertThat(PermissionEvaluationResult actual) {
        return new AuditDataPermissionResultAssert(actual);
    }

    public static ErrorResponseAssert assertApiException(ThrowableAssert.ThrowingCallable request) {
        return assertThatThrownBy(request)
                .asInstanceOf(new InstanceOfAssertFactory<>(SlglApiException.class, ErrorResponseAssert::new));
    }
}
