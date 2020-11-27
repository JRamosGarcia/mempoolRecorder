package com.mempoolrecorder;

import com.mempoolrecorder.components.containers.MempoolEventQueueContainer;
import com.mempoolrecorder.events.CustomChannels;
import com.mempoolrecorder.events.MempoolEvent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Component;

@Component
@EnableBinding(CustomChannels.class)
public class TxMempoolEventsHandler {

    @Value("${spring.cloud.stream.bindings.txMemPoolEvents.destination}")
    private String topic;

    @Autowired
    MempoolEventQueueContainer mempoolEventQueueContainer;

    @StreamListener("txMemPoolEvents")
    public void blockSink(MempoolEvent mempoolEvent) {
        mempoolEventQueueContainer.getBlockingQueue().add(mempoolEvent);
    }
}
