package com.mempoolrecorder.events;

import java.util.Optional;

import com.mempoolrecorder.bitcoindadapter.entities.blockchain.Block;
import com.mempoolrecorder.bitcoindadapter.entities.blocktemplate.BlockTemplateChanges;
import com.mempoolrecorder.bitcoindadapter.entities.mempool.TxPoolChanges;

import lombok.Getter;
import lombok.Setter;

/**
 * This class is an union of Block and TxPoolChanges since is meant to be used
 * as a kafka message for same topic conserving message order. The topic is a
 * "mempool event"
 */
@Getter
@Setter
public class MempoolEvent {
	//
	public enum EventType {
		NEW_BLOCK, REFRESH_POOL
	}

	private int seqNumber;
	private EventType eventType;
	private Block block;
	private TxPoolChanges txPoolChanges;
	private BlockTemplateChanges blockTemplateChanges;

	private MempoolEvent() {
	}

	public static MempoolEvent createFrom(TxPoolChanges txPoolChanges,
			Optional<BlockTemplateChanges> blockTemplateChanges, int seqNumber) {
		MempoolEvent mpe = new MempoolEvent();
		mpe.seqNumber = seqNumber;
		mpe.eventType = EventType.REFRESH_POOL;
		mpe.txPoolChanges = txPoolChanges;
		mpe.blockTemplateChanges = blockTemplateChanges.orElse(null);
		return mpe;
	}

	public static MempoolEvent createFrom(Block block, int seqNumber) {
		MempoolEvent mpe = new MempoolEvent();
		mpe.seqNumber = seqNumber;
		mpe.eventType = EventType.NEW_BLOCK;
		mpe.block = block;
		return mpe;
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

}
