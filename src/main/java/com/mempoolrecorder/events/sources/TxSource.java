package com.mempoolrecorder.events.sources;

import com.mempoolrecorder.events.MempoolEvent;

public interface TxSource {

	void publishMemPoolEvent(MempoolEvent memPoolEvent);
	
}