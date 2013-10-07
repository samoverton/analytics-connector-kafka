package com.acunu.analytics.kafka;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.acunu.analytics.conf.ConfigProperties;
import com.acunu.analytics.Flow;
import com.acunu.analytics.Context;
import com.acunu.analytics.Ingester;
import com.acunu.analytics.ingest.AbstractIngester;
import com.acunu.analytics.ingest.IngestException;

/**
 * An Analytics ingester that acts as a Kafka consumer. Each "flow" (implemented
 * by KafkaFlowSource) has an associated topic or topic filter, and a
 * configurable number of partitions, with each stream associated with a single
 * thread. The ingester machinery passes batches of events received on each
 * stream to a shared set of consumer threads that decode the events and pass
 * them on to the table or preprocessor specified in the flow.
 * 
 * TODO - would be nice to delay init until after all flows have been added.
 * TODO - callbacks on each batch to update
 * 
 * @author tmoreton
 * 
 */
public class KafkaIngester extends AbstractIngester {

	private Logger logger = LoggerFactory.getLogger(KafkaIngester.class);

	/**
	 * Timeout while waiting for messages.
	 */
	public static final int CONSUMER_ITERATOR_TIMEOUT = 1000;

	private ConsumerConnector consumer;

	/**
	 * Initialize and start-up the Ingester. Even though, it has started, it
	 * can't possibly serve any events unless some Flows are defined.
	 * 
	 * @param name
	 *            The ingester name
	 * 
	 * @param context
	 *            Execution context
	 */
	public KafkaIngester(String name, Context context) throws IngestException {
		super(name, context);
		assert consumer == null;

		// Pull all properties directly from ingestor config, replacing
		// underscores with dots.
		final Properties kafkaProps = new Properties();
		for (Map.Entry<String, Object> e : config.asMap().entrySet()) {
			String key = e.getKey().replace('_', '.');
			String value;
			if (e.getValue() instanceof Number) {
				value = e.getValue().toString();
			} else if (e.getValue() instanceof String) {
				value = (String) e.getValue();
			} else {
				throw new IngestException(String.format("Value for property %s must be a number or string", e.getKey()));
			}
			kafkaProps.setProperty(key, value);
		}

		// Check we have the required parameters.
		for (String param : new String[] { "groupid", "zk.connect" }) {
			if (!kafkaProps.containsKey(param))
				throw new IngestException("Missing required parameter " + param.replace('.', '_'));
		}

		// Insert our own stuff into the properties.
		kafkaProps.put("consumer.timeout.ms", String.valueOf(CONSUMER_ITERATOR_TIMEOUT));

		// Create connection.
		consumer = kafka.consumer.Consumer.createJavaConsumerConnector(new ConsumerConfig(kafkaProps));

		logger.info("Created Kafka consumer: " + consumer.toString());
	}

	/**
	 * Shutdown the ingester.
	 */
	@Override
	public void shutdown() {

		// Shutdown the flows, and then the consuming thread pool.
		super.shutdown();

		// Shutdown the consumer connection.
		if (this.consumer != null) {
			consumer.shutdown();
			this.consumer = null;
		}
	}

	/**
	 * Subscribe to a topic described by the flow.
	 */
	@Override
	protected KafkaFlowSource createFlowSource(Flow flow) {
		return new KafkaFlowSource(this, flow);
	}

	/**
	 * Create streams for an unclaimed topic.
	 */
	List<KafkaStream<Message>> createStreamsForTopic(String topic, int count) {
		// TODO for now, just one stream.
		final Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
		topicCountMap.put(topic, new Integer(count));
		Map<String, List<KafkaStream<Message>>> consumerMap = consumer.createMessageStreams(topicCountMap);
		return consumerMap.get(topic);
	}

	@Override
	public String toString() {
		return "KafkaIngester " + getName();
	}

}
