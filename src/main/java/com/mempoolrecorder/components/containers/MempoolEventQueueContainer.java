package com.mempoolrecorder.components.containers;

import java.util.concurrent.BlockingQueue;

import com.mempoolrecorder.events.MempoolEvent;

public interface MempoolEventQueueContainer {

    BlockingQueue<MempoolEvent> getBlockingQueue();

}
