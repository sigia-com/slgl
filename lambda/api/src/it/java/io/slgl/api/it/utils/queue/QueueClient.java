package io.slgl.api.it.utils.queue;

import com.amazonaws.AbortedException;
import io.slgl.api.it.properties.Props;
import io.slgl.api.utils.json.UncheckedObjectMapper;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import static software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT;

@Slf4j
public class QueueClient<T> implements Closeable {

	private static final String CONNECTION_POOL_SHUT_DOWN = "Connection pool shut down";
	private static final Integer DEFAULT_WAIT_TIME_SECONDS = 1;

	private final SqsClient amazonSqs;
	private final String queueUrl;
	private final Integer waitTimeSeconds;
	private final Class<T> messageClass;

	private final List<Thread> pollThreads = new ArrayList<>();

	private volatile boolean shutdown = false;

	public QueueClient(SqsClient amazonSqs, String queueUrl, Class<T> messageClass) {
		this(amazonSqs, queueUrl, messageClass, DEFAULT_WAIT_TIME_SECONDS);
	}

	public QueueClient(SqsClient amazonSqs, String queueUrl, Class<T> messageClass, Integer waitTimeSeconds) {
		this.amazonSqs = amazonSqs;
		this.queueUrl = queueUrl;
		this.waitTimeSeconds = waitTimeSeconds;
		this.messageClass = messageClass;

		log.info("New queueClient [queueUrl={}, messageClass={}, waitTimeSeconds={}]",
				queueUrl, messageClass.getSimpleName(), waitTimeSeconds);
	}

	public static <T> QueueClient<T> start(Class<T> messageClass, MessageProcessor<T> processor) {
		QueueClient<T> queueClient = new QueueClient<>(
				SqsClient.create(),
				Props.getSlglProperties().getAuditorSqsQueueUrl(),
				messageClass
		);
		queueClient.poll(processor);
		return queueClient;
	}

	public synchronized void poll(final MessageProcessor<T> messageProcessor) {
		Thread pollThread = new Thread(() -> pollInThread(messageProcessor));
		pollThreads.add(pollThread);
		pollThread.start();
	}

	private void pollInThread(MessageProcessor<T> messageProcessor) {
		log.info("Polling thread started for message {}", messageClass.getSimpleName());
		try {
			while (!shutdown) {
				receiveAndProcess(messageProcessor);
			}
		} finally {
			log.info("Polling thread stopped for message {}", messageClass.getSimpleName());
		}
	}

	private void receiveAndProcess(MessageProcessor<T> messageProcessor) {
		Message message = null;
		try {
			message = waitForMessage();
			if (message == null) {
				return;
			}

			try {
				String body = message.body();
				int approximateReceiveCount = Integer.parseInt(message.attributes().get(APPROXIMATE_RECEIVE_COUNT));
				log.info("Got queue message: {}", message);
				messageProcessor.processMessage(UncheckedObjectMapper.MAPPER.readValue(body, messageClass), message.receiptHandle(), approximateReceiveCount);
			} finally {
				remove(message.receiptHandle());
			}

		} catch (QueueDoesNotExistException ex) {
			log.error("Queue {} does not exist", queueUrl);

		} catch (Exception ex) {
			if (shutdown && (CONNECTION_POOL_SHUT_DOWN.equals(ex.getMessage()) || ex instanceof AbortedException)) {
				log.debug(ex.getMessage(), ex);
				return;
			}
			log.error("Unexpected error for: queue='{}', message='{}'", queueUrl, message, ex);
		}
	}

	private Message waitForMessage() {
		ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
				.queueUrl(queueUrl)
				.maxNumberOfMessages(1)
				.waitTimeSeconds(waitTimeSeconds)
				.attributeNamesWithStrings("ApproximateReceiveCount")
				.build();

		List<Message> messages = amazonSqs.receiveMessage(receiveMessageRequest).messages();
		if (messages.isEmpty()) {
			return null;
		}

		log.trace("Received message: {}", messages.get(0));
		return messages.get(0);
	}

	private void remove(String messageReceiptHandle) {
		log.trace("Removing message: {}", messageReceiptHandle);
		var deleteMessageRequest = DeleteMessageRequest.builder()
				.queueUrl(queueUrl)
				.receiptHandle(messageReceiptHandle)
				.build();
		amazonSqs.deleteMessage(deleteMessageRequest);
	}

	@Override
	public void close() {
		synchronized (this) {
			shutdown = true;
			for (Thread t : pollThreads) {
				t.interrupt();
			}
			for (Thread t : pollThreads) {
				try {
					t.join();
				} catch (InterruptedException e) {
					log.warn("Interrupted on shutdown", e);
				}
			}
			pollThreads.clear();
		}
	}
}
