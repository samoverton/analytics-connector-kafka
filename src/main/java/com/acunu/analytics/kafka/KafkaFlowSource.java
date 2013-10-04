package com.acunu.analytics.kafka;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kafka.consumer.ConsumerIterator;
import kafka.consumer.ConsumerTimeoutException;
import kafka.consumer.KafkaStream;
import kafka.message.Message;

import com.acunu.analytics.Flow;
import com.acunu.analytics.ingest.FlowSource;
import com.acunu.analytics.ingest.IngestException;

/**
 * State associated with a flow for a Kafka topic
 * 
 * @author tmoreton
 * 
 */
public class KafkaFlowSource extends FlowSource<KafkaIngester> {

	private final Logger logger = LoggerFactory.getLogger(KafkaFlowSource.class);

	public static final String KEY_TOPIC = "topic";

	public static final String KEY_MAX_BATCH_SIZE = "max_batch_size";

	public static final int DEFAULT_MAX_BATCH_SIZE = 50;

	private final String topic;

	public KafkaFlowSource(KafkaIngester ingester, Flow flow) {

		super(ingester, flow); // TODO streams->threads

		this.topic = flow.getProperties().getString(KEY_TOPIC);
		if (this.topic == null) {
			throw new IllegalArgumentException("Must specify topic in flow properties");
		}

		this.maxBatchSize = flow.getProperties().getInteger(KEY_MAX_BATCH_SIZE, DEFAULT_MAX_BATCH_SIZE);
	}

	private int maxBatchSize;

	@Override
	public void start() throws IngestException {

		// TODO proper checking of params earlier.
		// TODO Get the topic, number of partitions.

		// TODO check other flows aren't using the same topic.

		super.start();
	}

	private ConsumerIterator<Message> it;

	/**
	 * Ingest another batch of events, up to the max_batch_size property, and
	 * timing out if a new event is not received within the Kafka consumer
	 * timeout window.
	 */
	@Override
	public List<?> ingestSomeMore() throws IngestException, InterruptedException {

		if (this.it == null) {
			synchronized (ingester) {
				if (this.it == null) {
					// Create the streams for this topic.
					List<KafkaStream<Message>> streams = ingester.createStreamsForTopic(topic, 1);
					this.it = streams.get(0).iterator();
				}
			}
		}

		final List<Object> batch = new ArrayList<Object>(this.maxBatchSize);

		int msgs_in_this_batch = 0;
		while (this.it != null && msgs_in_this_batch < this.maxBatchSize) {
			try {
				// Get the next message. We might timeout waiting for it. That's
				// fine.
				ByteBuffer buffer = it.next().message().payload();
				final byte[] bytes = new byte[buffer.remaining()];
				buffer.get(bytes);
				batch.add(bytes);
				msgs_in_this_batch++;

				logger.debug(String.format("Got message on topic %s, length %d", this.topic, bytes.length));
			} catch (ConsumerTimeoutException e) {
				// No more messages for now.
				break;
			}
		}

		return batch;
	}

}
