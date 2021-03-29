package io.slgl.api.it.utils.queue;

public interface MessageProcessor<T> {

    void processMessage(T message, String messageReceiptHandle, int approximateReceiveCount) throws Exception;
}
