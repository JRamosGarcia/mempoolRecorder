package com.mempoolrecorder.events.sinks;

import org.apache.kafka.clients.consumer.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;

import com.mempoolrecorder.events.CustomChannels;
import com.mempoolrecorder.events.MempoolEvent;

@EnableBinding(CustomChannels.class)
public class TxMemPoolEventsHandler {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@StreamListener("txMemPoolEvents")
	public void blockSink(MempoolEvent mempoolEvent, @Header(KafkaHeaders.CONSUMER) Consumer<?, ?> consumer) {
		try {
			//TODO no se reciben msgs
			if (mempoolEvent.getEventType() == MempoolEvent.EventType.NEW_BLOCK) {
				logger.info("New mempoolEvent has arrived with block: {}",
						mempoolEvent.tryGetBlock().get().getHeight());
			} else if (mempoolEvent.getEventType() == MempoolEvent.EventType.REFRESH_POOL) {
				logger.info("New mempoolEvent with txPoolchanges({})",
						mempoolEvent.tryGetTxPoolChanges().get().getChangeCounter());
			}
		} catch (Exception e) {
			logger.error("Exception: ", e);
		}
	}
}
