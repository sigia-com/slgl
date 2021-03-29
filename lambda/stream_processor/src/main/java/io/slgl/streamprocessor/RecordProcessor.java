package io.slgl.streamprocessor;

import com.amazonaws.services.kinesis.clientlibrary.types.UserRecord;
import com.amazonaws.services.kinesis.model.Record;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import com.fasterxml.jackson.dataformat.ion.ionvalue.IonValueMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import io.slgl.streamprocessor.message.Block;
import io.slgl.streamprocessor.message.Document;
import io.slgl.streamprocessor.message.Message;
import io.slgl.streamprocessor.model.*;
import io.slgl.streamprocessor.utils.LambdaEnv;
import lombok.Data;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RecordProcessor {

    private static final int MAX_MESSAGE_SIZE = 256 * 1024;
    private static final List<String> TABLES = ImmutableList.of("node", "link");

    private final IonValueMapper ionMapper = ExecutionContext.getIonMapper();
    private final ObjectMapper objectMapper = ExecutionContext.getObjectMapper();

    private final SnsClient amazonSNS = ExecutionContext.getAmazonSNS();

    private final State state = new State();

    public void processRecord(Record rawRecord) throws IOException {
        for (UserRecord record : UserRecord.deaggregate(ImmutableList.of(rawRecord))) {
            processRecord(record);
        }
    }

    public void sendRemainingMessage() throws JsonProcessingException {
        if (state.getMessage() == null) {
            return;
        }

        sendMessage(state.getMessage());
        state.setMessage(null);
    }

    private void processRecord(UserRecord rawRecord) throws IOException {
        QldbRecord record = ionMapper.readValue(new ByteBufferBackedInputStream(rawRecord.getData()), QldbRecord.class);

        if (record.getRecordType() == RecordType.BLOCK_SUMMARY) {
            processBlockSummaryRecord(record);
        } else if (record.getRecordType() == RecordType.REVISION_DETAILS) {
            processRevisionDetailsRecord(record);
        }
    }

    private void processBlockSummaryRecord(QldbRecord record) throws JsonProcessingException {
        RecordPayload payload = record.getPayload();

        Block block = new Block()
                .setStrandId(payload.getBlockAddress().getStrandId())
                .setSequenceNo(payload.getBlockAddress().getSequenceNo())
                .setDocumentIds(nullToEmptyList(payload.getRevisionSummaries()).stream()
                        .map(RevisionSummary::getDocumentId)
                        .collect(Collectors.toList()));

        addBlockToMessage(block);
    }

    private void processRevisionDetailsRecord(QldbRecord record) throws JsonProcessingException {
        String tableName = record.getPayload().getTableInfo().getTableName();

        Revision revision = record.getPayload().getRevision();

        Document document = new Document()
                .setStrandId(revision.getBlockAddress().getStrandId())
                .setSequenceNo(revision.getBlockAddress().getSequenceNo())
                .setTable(tableName)
                .setDocumentId(revision.getMetadata().getId());

        if (TABLES.contains(tableName)) {
            document.setData(revision.getData());
        }

        addEntryToMessage(document);
    }

    private void addBlockToMessage(Block block) throws JsonProcessingException {
        Message message = state.getMessage();

        if (message == null) {
            state.setMessage(new Message().addBlock(block));
            return;
        }

        Message newMessage = message.addBlock(block);

        if (isMessageTooBig(newMessage)) {
            sendMessage(message);
            state.setMessage(new Message().addBlock(block));
            return;
        }

        state.setMessage(newMessage);
    }

    private void addEntryToMessage(Document document) throws JsonProcessingException {
        Message message = state.getMessage();

        if (message == null) {
            state.setMessage(new Message().addDocument(document));
            return;
        }

        Message newMessage = message.addDocument(document);

        if (isMessageTooBig(newMessage)) {
            sendMessage(message);
            state.setMessage(new Message().addDocument(document));
            return;
        }

        state.setMessage(newMessage);
    }

    public void sendMessage(Message message) throws JsonProcessingException {
        String messageJson = objectMapper.writeValueAsString(message);

        var request = PublishRequest.builder()
                .topicArn(LambdaEnv.getEntriesSnsTopic())
                .message(messageJson)
                .build();

        amazonSNS.publish(request);
    }

    public boolean isMessageTooBig(Message message) throws JsonProcessingException {
        String messageJson = objectMapper.writeValueAsString(message);
        byte[] messageBytes = messageJson.getBytes(Charsets.UTF_8);

        return messageBytes.length >= MAX_MESSAGE_SIZE;
    }

    private static <T> List<T> nullToEmptyList(List<T> list) {
        return list != null ? list : Collections.emptyList();
    }

    @Data
    private class State {
        private Message message;
    }
}
