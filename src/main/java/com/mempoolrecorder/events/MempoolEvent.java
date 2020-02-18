package com.mempoolrecorder.events;

import java.util.Optional;

import com.mempoolrecorder.bitcoindadapter.entities.blockchain.Block;
import com.mempoolrecorder.bitcoindadapter.entities.blocktemplate.BlockTemplateChanges;
import com.mempoolrecorder.bitcoindadapter.entities.mempool.TxPoolChanges;

/**
 * This class is an union of Block and TxPoolChanges since is meant to be used
 * as a kafka message for same topic conserving message order. The topic is a
 * "mempool event"
 */
public class MempoolEvent {
	//
	public enum EventType {
		NEW_BLOCK, REFRESH_POOL
	};

	private EventType eventType;
	private Block block;
	private TxPoolChanges txPoolChanges;
	private BlockTemplateChanges blockTemplateChanges;

	private MempoolEvent() {
	}

	public EventType getEventType() {
		return eventType;
	}
	
	public Optional<Block> tryGetBlock() {
		if (this.eventType != null && this.eventType == EventType.NEW_BLOCK) {
			return Optional.ofNullable(block);
		}
		return Optional.empty();
	}

	public Optional<TxPoolChanges> tryGetTxPoolChanges() {
		if (this.eventType != null && this.eventType == EventType.REFRESH_POOL) {
			return Optional.ofNullable(txPoolChanges);
		}
		return Optional.empty();
	}

	public Optional<BlockTemplateChanges> tryGetBlockTemplateChanges() {
		if (this.eventType != null && this.eventType == EventType.REFRESH_POOL) {
			return Optional.ofNullable(blockTemplateChanges);
		}
		return Optional.empty();
	}
	
	public void setEventType(EventType eventType) {
		this.eventType = eventType;
	}

	public void setBlock(Block block) {
		this.block = block;
	}

	public void setTxPoolChanges(TxPoolChanges txPoolChanges) {
		this.txPoolChanges = txPoolChanges;
	}

	public void setBlockTemplateChanges(BlockTemplateChanges blockTemplateChanges) {
		this.blockTemplateChanges = blockTemplateChanges;
	}

}
