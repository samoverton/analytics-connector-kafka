package com.acunu.analytics.kafka;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import kafka.consumer.ConsumerIterator;
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

	public KafkaFlowSource(KafkaIngester ingester, Flow flow) {
		super(ingester, flow); // TODO streams->threads
	}

	private int batch_size = 50;

	@Override
	public void start() throws IngestException {
		super.start();

		// TODO Get the topic, number of partitions.
		final String topic = flow.getName();

		// Create the streams for this topic.
		List<KafkaStream<Message>> streams = ingester.createStreamsForTopic(topic, 1);
		this.it = streams.get(0).iterator();
	}

	private ConsumerIterator<Message> it;

	@Override
	public List<?> ingestSomeMore() throws IngestException, InterruptedException {
		final List<Object> batch = new ArrayList<Object>(batch_size);
		for (int i = 0; it.hasNext() && i < this.batch_size; i++) {
			ByteBuffer buffer = it.next().message().payload();
			final byte[] bytes = new byte[buffer.remaining()];
			buffer.get(bytes);
			batch.add(bytes);
		}
		return batch;
	}

}
