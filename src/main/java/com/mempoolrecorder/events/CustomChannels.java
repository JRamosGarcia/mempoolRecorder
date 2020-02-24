package com.mempoolrecorder.events;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface CustomChannels {
	@Input("txMemPoolEvents")
	SubscribableChannel orgs();

	@Qualifier("sonbEventsChannel")
	@Output("sonbEvents")
    MessageChannel sonbEventsChannel();

}
