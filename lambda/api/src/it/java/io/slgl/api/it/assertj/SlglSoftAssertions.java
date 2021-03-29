package io.slgl.api.it.assertj;

import io.slgl.client.audit.PermissionAuditMessage;
import io.slgl.client.audit.PermissionEvaluationResult;
import io.slgl.client.error.ErrorResponse;
import io.slgl.client.error.SlglApiException;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.ThrowableAssert;

import java.util.function.Consumer;

public class SlglSoftAssertions extends SoftAssertions {

    public static void softly(Consumer<SlglSoftAssertions> asserts) {
        var softly = new SlglSoftAssertions();
        asserts.accept(softly);
        softly.assertAll();
    }

    public ErrorAssert assertThat(ErrorResponse actual) {
        return proxy(ErrorAssert.class, ErrorResponse.class, actual);
    }

    public AuditDataAssert assertThat(PermissionAuditMessage actual) {
        return proxy(AuditDataAssert.class, PermissionAuditMessage.class, actual);
    }

    public AuditDataPermissionResultAssert assertThat(PermissionEvaluationResult actual) {
        return proxy(AuditDataPermissionResultAssert.class, PermissionEvaluationResult.class, actual);
    }

    public ErrorResponseAssert assertApiException(ThrowableAssert.ThrowingCallable request) {
        return assertThatThrownBy(request)
                .asInstanceOf(new InstanceOfAssertFactory<>(SlglApiException.class, ErrorResponseAssert::new));
    }
}
