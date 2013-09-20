package com.acunu.analytics.kafka;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.Message;

import com.acunu.analytics.Config;
import com.acunu.analytics.Flow;
import com.acunu.analytics.ingest.AbstractIngester;

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

	private ConsumerConnector consumer;

	/**
	 * Initialize and start-up the Ingester. Even though, it has started, it
	 * can't possibly serve any events unless some Flows are defined.
	 * 
	 * @param ingesterProperties
	 *            Ingester specific properties
	 * 
	 * @param aaConfig
	 *            jBird config could be used by Ingester.
	 */
	public void init(Map<String, Object> ingesterProperties, Config aaConfig) {
		super.init(ingesterProperties, aaConfig);

		if (this.consumer == null) {
			// Pull all properties directly from ingestor config
			Properties props = new Properties();
			props.putAll(ingesterProperties);

			// Create connection.
			consumer = kafka.consumer.Consumer.createJavaConsumerConnector(new ConsumerConfig(props));
		}
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

}
