package com.acunu.analytics.kafka;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.ConsumerTimeoutException;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.acunu.analytics.Flow;
import com.acunu.analytics.ingest.FlowSource;
import com.acunu.analytics.ingest.IngestException;

/**
 * State associated with a flow for a Kafka topic.
 * 
 * @author tmoreton
 * 
 */
public class KafkaFlowSource extends FlowSource<KafkaIngester> {

	private final Logger logger = LoggerFactory.getLogger(KafkaFlowSource.class);

	public static final String KEY_TOPIC = "topic";
	public static final String KEY_GROUP_ID = "groupid";

	public static final String KEY_MAX_BATCH_SIZE = "max_batch_size";

	public static final int DEFAULT_MAX_BATCH_SIZE = 50;

	private final String topic;
	
	private final String groupId;

	public KafkaFlowSource(KafkaIngester ingester, Flow flow) {

		super(ingester, flow); // TODO streams->threads

		this.topic = flow.getProperties().getString(KEY_TOPIC);
		if (this.topic == null) {
			throw new IllegalArgumentException("Missing required flow property '" + KEY_TOPIC + "'"); 
		}

		this.groupId = flow.getProperties().getString(KEY_GROUP_ID);
		if (this.groupId == null) {
			throw new IllegalArgumentException("Missing required flow property '" + KEY_GROUP_ID + "'"); 
		}

		this.maxBatchSize = flow.getProperties().getInteger(KEY_MAX_BATCH_SIZE, DEFAULT_MAX_BATCH_SIZE);
	}

	/**
	 * Connector to the Kafka server
	 */
	private ConsumerConnector consumer;

	private KafkaStream<Message> stream;

	private int maxBatchSize;

	@Override
	public void start() throws IngestException {

		// Create consumer connection.
		final Properties props = new Properties();
		props.putAll(ingester.getKafkaProps());
		props.setProperty("groupid", groupId);
		consumer = kafka.consumer.Consumer.createJavaConsumerConnector(new ConsumerConfig(props));

		logger.info("Created Kafka consumer: {}", consumer.toString());

		// Register the partition.
		final Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
		topicCountMap.put(topic, new Integer(1));
		Map<String, List<KafkaStream<Message>>> consumerMap = consumer.createMessageStreams(topicCountMap);
		stream = consumerMap.get(topic).get(0);

		logger.info("Created Kafka stream for topic '{}'", topic);

		super.start();
	}

	@Override
	public void stop() throws InterruptedException {
		super.stop();

		if (consumer != null) {
			this.stream = null;

			logger.info("Shutting down Kafka consumer: {}", consumer.toString());

			consumer.shutdown();
		}
	}

	/**
	 * Ingest another batch of events, up to the max_batch_size property, and
	 * timing out if a new event is not received within the Kafka consumer
	 * timeout window.
	 */
	@Override
	public List<?> ingestSomeMore() throws IngestException, InterruptedException {

		final List<Object> batch = new ArrayList<Object>(this.maxBatchSize);

		int msgs_in_this_batch = 0;
		while (msgs_in_this_batch < this.maxBatchSize) {
			try {
				if (this.stream == null)
					break;

				ConsumerIterator<Message> it = this.stream.iterator();
				if (it == null)
					break;

				// Get the next message. Fine if we timeout waiting for it.
				final Message msg = it.next().message();
				if (msg == null || !msg.isValid()) {
					logger.debug("Skipping invalid message on {}" + this.topic);
					continue;
				}
				
				// It's a valid message. Add it to the batch.
				final ByteBuffer buffer = msg.payload();
				final byte[] bytes = new byte[buffer.remaining()];
				buffer.get(bytes);
				batch.add(bytes);
				msgs_in_this_batch++;

				if (logger.isDebugEnabled())
					logger.debug(String.format("Got message on topic %s, length %d", this.topic, bytes.length));
				
			} catch (ConsumerTimeoutException e) {
				// No more messages for now.
				break;
			}
		}

		return batch;
	}
}
