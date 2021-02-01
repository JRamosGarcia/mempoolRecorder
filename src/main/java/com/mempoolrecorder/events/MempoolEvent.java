package com.mempoolrecorder.events;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mempoolrecorder.bitcoindadapter.entities.Transaction;
import com.mempoolrecorder.bitcoindadapter.entities.blockchain.Block;
import com.mempoolrecorder.bitcoindadapter.entities.mempool.TxPoolChanges;
import com.mempoolrecorder.entities.BlockTemplate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class is an union of Block, TxPoolChanges and BlockTemplate since is
 * meant to be used as a kafka message for same topic conserving message order.
 * The topic is a "mempool event"
 * 
 * Two kind of Events can be used: NEW_BLOCK and REFRESH_POOL. when NEW_BLOCK is
 * sent, Block, TxPoolChanges and blockTemplate are sent. when REFRESH_POOL is
 * sent, only txPoolChanges is sent.
 * 
 * mempoolSize is sent to know when downstream apps are syncronized with us.
 * 
 */
@Getter
@Setter
@NoArgsConstructor
public class MempoolEvent {
	//
	public enum EventType {
		NEW_BLOCK, REFRESH_POOL
	}

	private int seqNumber;
	private int mempoolSize;// This is the mempool size when this MeempoolEvent has been applied.
	private EventType eventType;
	private TxPoolChanges txPoolChanges;
	private Optional<Block> block;
	private Optional<BlockTemplate> blockTemplate;

	public static MempoolEvent createFrom(TxPoolChanges txPoolChanges, int seqNumber, int mempoolSize) {
		MempoolEvent mpe = new MempoolEvent();
		mpe.eventType = EventType.REFRESH_POOL;
		mpe.txPoolChanges = txPoolChanges;
		mpe.seqNumber = seqNumber;
		mpe.mempoolSize = mempoolSize;
		return mpe;
	}

	public static MempoolEvent createFrom(Block block, TxPoolChanges txPoolChanges,
			Optional<BlockTemplate> blockTemplate, int seqNumber, int mempoolSize) {
		MempoolEvent mpe = new MempoolEvent();
		mpe.eventType = EventType.NEW_BLOCK;
		mpe.txPoolChanges = txPoolChanges;
		mpe.blockTemplate = blockTemplate;
		mpe.block = Optional.of(block);
		mpe.seqNumber = seqNumber;
		mpe.mempoolSize = mempoolSize;
		return mpe;
	}

	/**
	 * Returns blocks transactions in connected or disconnected blocks
	 */
	public Optional<List<String>> tryGetBlockTxIds() {
		if (eventType != null && eventType == EventType.NEW_BLOCK && block.isPresent()) {
			if (Boolean.TRUE.equals(block.get().getConnected())) {
				return Optional.of(txPoolChanges.getRemovedTxsId());
			} else {
				return Optional
						.of(txPoolChanges.getNewTxs().stream().map(Transaction::getTxId).collect(Collectors.toList()));
			}
		}
		return Optional.empty();
	}
}
