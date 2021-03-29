package io.slgl.api.permission.service;

import io.slgl.api.ExecutionContext;
import io.slgl.api.config.Provider;
import io.slgl.api.model.AuditorEntity;
import io.slgl.api.repository.NodeEntity;
import io.slgl.api.service.LinksGetter;
import io.slgl.api.utils.json.UncheckedObjectMapper;
import io.slgl.client.audit.PermissionAudit;
import io.slgl.client.audit.PermissionAuditMessage;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;

@Slf4j
public class AuditorNotifier {

    private final LinksGetter linksGetter = ExecutionContext.get(LinksGetter.class);
    private final Provider<SqsClient> amazonSQS = ExecutionContext.getProvider(SqsClient.class);

    private final UncheckedObjectMapper mapper = new UncheckedObjectMapper()
            .disable(SORT_PROPERTIES_ALPHABETICALLY);

    public void notify(NodeEntity node, PermissionAudit permissionAudit) {
        if (node == null || (permissionAudit.getEvaluatedPermissions().isEmpty() && permissionAudit.getEvaluationLog().isEmpty())) {
            return;
        }

        for (AuditorEntity auditor : linksGetter.getAuditors(node)) {
            var policy = auditor.getAuditPolicy();
            if (policy.shouldSendAudit(permissionAudit)) {
                sendNotification(auditor, permissionAudit);
            } else {
                log.info("Auditor ignored by policy | policy: {} | request_type: {} | queue: {}", policy, permissionAudit.getRequestType(), auditor.getAwsSqs());
            }
        }
    }

    private void sendNotification(AuditorEntity auditor, PermissionAudit permissionAudit) {
        try {
            var auditData = new PermissionAuditMessage(auditor.getAuthorizedUser(), permissionAudit);
            send(auditor.getAwsSqs(), auditData);
            log.info("Sent audit data | request_type: {} | queue: {} ", permissionAudit.getRequestType(), auditor.getAwsSqs());
        } catch (Throwable ex) {
            log.info("Error sending audit data | request_type: {} | queue: {} ", permissionAudit.getRequestType(), auditor.getAwsSqs(), ex);
        }
    }

    private void send(String sqsUrl, PermissionAuditMessage permissionAuditMessage) {
        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(sqsUrl)
                .messageBody(mapper.writeValueAsString(permissionAuditMessage))
                .build();
        amazonSQS.get().sendMessage(request);
    }
}
