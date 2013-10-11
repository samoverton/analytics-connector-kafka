package com.acunu.analytics.kafka;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.acunu.analytics.Context;
import com.acunu.analytics.Flow;
import com.acunu.analytics.ingest.AbstractIngester;
import com.acunu.analytics.ingest.IngestException;

/**
 * An experimental Analytics ingester for Apache Kafka. Each "flow" (implemented
 * by KafkaFlowSource) has an associated Kafka consumer, and a topic, group, and
 * a map of hostnames to partitions. The ingester machinery passes batches of
 * events received on each stream to a shared set of consumer threads that
 * decode the events and pass them on to the table or preprocessor specified in
 * the flow.
 * 
 * TODO - callbacks on each batch to update. Right now we rely on periodic push
 * off offsets into ZK.
 * 
 * @author tmoreton
 * 
 */
public class KafkaIngester extends AbstractIngester {

	private Logger logger = LoggerFactory.getLogger(KafkaIngester.class);

	/**
	 * Timeout while waiting for messages. We don't want to delay batches of
	 * messages for too long.
	 */
	public static final int CONSUMER_ITERATOR_TIMEOUT = 1000;

	/**
	 * Default properties for Kafka consumers, augmented by each flow.
	 */
	private final Map<String,Object> kafkaProps;
	
	Map<String,Object> getKafkaProps() {
		return kafkaProps;
	}

	/**
	 * Initialize and start-up the Ingester. Even though it has started, it
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

		// Pull all properties directly from ingestor config, replacing
		// underscores with dots.
		this.kafkaProps = new HashMap<String,Object>();
		for (Map.Entry<String, Object> e : context.getConfig().asMap().entrySet()) {
			kafkaProps.put(e.getKey().replace('_', '.'), e.getValue());
		}

		// Check we have the required parameters.
		for (String param : new String[] { "zk.connect" }) {
			if (!kafkaProps.containsKey(param))
				throw new IngestException(String.format("Missing required property '%s'", param.replace('.', '_')));
		}

		// Override any specified timeout in the properties. Sorry.
		kafkaProps.put("consumer.timeout.ms", String.valueOf(CONSUMER_ITERATOR_TIMEOUT));

	}

	/**
	 * Subscribe to a topic described by the flow.
	 */
	@Override
	protected KafkaFlowSource createFlowSource(Flow flow) {
		return new KafkaFlowSource(this, flow);
	}

	@Override
	public String toString() {
		return "KafkaIngester " + getName();
	}

}
