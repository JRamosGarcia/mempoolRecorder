package com.mempoolrecorder.events;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

public interface CustomChannels {
	@Input("txMemPoolEvents")
	SubscribableChannel orgs();
}
