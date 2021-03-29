package io.slgl.api.it;

import io.slgl.api.it.properties.Props;
import io.slgl.api.utils.ErrorCode;
import io.slgl.client.node.*;
import org.junit.jupiter.api.Test;

import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.it.data.PermissionMother.allowAllForEveryone;
import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;
import static io.slgl.client.node.AuditorNodeRequest.AuditPolicy.ALL;

public class AuditorIT extends AbstractApiTest {

    private static final String AUDITOR_SQS_QUEUE_URL = Props.getSlglProperties().getAuditorSqsQueueUrl();

    @Test
    public void shouldCreateNodeWithInlineAuditor() {
        // given
        NodeRequest.Builder<?> request = NodeRequest.builder()
                .id(generateUniqueId())
                .linkAuditor(AuditorNodeRequest.builder()
                        .auditPolicy(ALL)
                        .awsSqs(AUDITOR_SQS_QUEUE_URL));

        // when
        NodeResponse response = ledger.writeNode(request);

        // then
        assertThat(response).isNotNull();
    }

    @Test
    public void shouldCreateNodeWithLinkedAuditor() {
        // given
        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .permission(allowAllForEveryone())));

        WriteRequest request = WriteRequest.builder()
                .addRequest(AuditorNodeRequest.builder()
                        .auditPolicy(ALL)
                        .awsSqs(AUDITOR_SQS_QUEUE_URL))
                .addLinkRequest(0, node, "#auditors")
                .build();

        // when
        WriteResponse response = ledger.write(request);

        // then
        assertThat(response).isNotNull();
    }

    @Test
    public void shouldFailWhenMoreThanOneAuditorIsAddedToNode() {
        // given
        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .linkAuditor(user.auditor(ALL))
                .linkAuditor(user.auditor(ALL))
                .build();

        // when
        var error = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldValidateAuditorData() {
        // given
        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .linkAuditor(AuditorNodeRequest.builder()
                        .awsSqs("invalid_url"))
                .build();

        // when
        var error = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(error).hasErrorCode(ErrorCode.VALIDATION_ERROR)
                .hasFieldError("requests[0].#auditors[0].audit_policy", "not_null")
                .hasFieldError("requests[0].#auditors[0].aws_sqs", "pattern");
    }
}
