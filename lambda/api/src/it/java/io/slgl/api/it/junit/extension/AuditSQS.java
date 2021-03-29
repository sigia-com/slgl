package io.slgl.api.it.junit.extension;

import com.google.common.base.Preconditions;
import io.slgl.api.it.utils.queue.QueueClient;
import io.slgl.client.audit.PermissionAuditMessage;
import io.slgl.client.protocol.Identified;
import io.slgl.client.utils.Experimental;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static com.google.common.base.Objects.equal;

@Experimental
public class AuditSQS {

    private final QueueClient<PermissionAuditMessage> client;
    private final List<PermissionAuditMessage> messages = new CopyOnWriteArrayList<>();

    AuditSQS() {
        this.client = QueueClient.start(PermissionAuditMessage.class, this::processMessage);
    }

    private void processMessage(PermissionAuditMessage message, String messageReceiptHandle, int approximateReceiveCount) {
        messages.add(message);
    }

    public PermissionAuditMessage getSingleByNodeAndAnchor(Identified identified, String anchor) {
        var results = getByNodeAndAnchor(identified.getId(), anchor);
        Preconditions.checkState(results.size() > 0);
        return results.get(0);
    }

    public List<PermissionAuditMessage> getByNodeAndAnchor(Identified identified, String anchor) {
        return getByNodeAndAnchor(identified.getId(), anchor);
    }

    public List<PermissionAuditMessage> getByNode(Identified identified) {
        return getByNodeAndAnchor(identified.getId(), null);
    }

    public List<PermissionAuditMessage> getByNodeAndAnchor(String node, String anchor) {
        return messages.stream()
                .filter(it -> equal(it.getPermissionAudit().getNode(), node) && equal(it.getPermissionAudit().getAnchor(), anchor))
                .collect(Collectors.toList());
    }

    void close() {
        client.close();
    }
}
