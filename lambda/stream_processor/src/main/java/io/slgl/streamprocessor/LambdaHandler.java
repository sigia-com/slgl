package io.slgl.streamprocessor;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LambdaHandler implements RequestHandler<KinesisEvent, Object> {

    @Override
    public Object handleRequest(KinesisEvent request, Context context) {

        try {
            log.info("Request = {}", request);

            RecordProcessor recordProcessor = new RecordProcessor();
            for (KinesisEvent.KinesisEventRecord record : request.getRecords()) {
                recordProcessor.processRecord(record.getKinesis());
            }
            recordProcessor.sendRemainingMessage();

            return null;

        } catch (Exception e) {
            log.error("Lambda execution failed with unexpected error", e);

            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }
}
