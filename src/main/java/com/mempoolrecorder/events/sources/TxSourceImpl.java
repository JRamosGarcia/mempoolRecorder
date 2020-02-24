package com.mempoolrecorder.events.sources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;

import com.mempoolrecorder.events.CustomChannels;
import com.mempoolrecorder.events.MempoolEvent;

@EnableBinding(CustomChannels.class)
public class TxSourceImpl implements TxSource {

	@Autowired
	@Qualifier("sonbEventsChannel")
	private MessageChannel sonbEventsChannel;

	@Override
	public void publishMemPoolEvent(MempoolEvent memPoolEvent) {
		sonbEventsChannel.send(MessageBuilder.withPayload(memPoolEvent).build());
		
	}

}
