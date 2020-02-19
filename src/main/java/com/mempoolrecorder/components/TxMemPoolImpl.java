package com.mempoolrecorder.components;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.mempoolrecorder.bitcoindadapter.entities.Transaction;
import com.mempoolrecorder.bitcoindadapter.entities.mempool.TxPoolChanges;

@Component
public class TxMemPoolImpl implements TxMemPool {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private ConcurrentHashMap<String, Transaction> txMempool = new ConcurrentHashMap<>();

	@Override
	public void refresh(TxPoolChanges txPoolChanges) {
		txPoolChanges.getNewTxs().stream().forEach(tx -> {
			txMempool.put(tx.getTxId(), tx);
		});
		txPoolChanges.getRemovedTxsId().stream().forEach(txId -> {
			Transaction tx = txMempool.remove(txId);
			if (null == tx) {
				logger.info("Removing non existing tx from mempool, txId: {}", txId);
			}
		});
		txPoolChanges.getTxAncestryChangesMap().entrySet().stream().forEach(entry -> {
			Transaction oldTx = txMempool.remove(entry.getKey());
			if (null == oldTx) {
				logger.info("Non existing txKey in txMemPool for update, txId: {}", entry.getKey());
				return;
			}
			// remove+put must be made each modification since tx modification while on map
			// is pretty unsafe. (suffered in my own skin)
			oldTx.setFees(entry.getValue().getFees());
			oldTx.setTxAncestry(entry.getValue().getTxAncestry());
			txMempool.put(oldTx.getTxId(), oldTx);
		});

		logTxPoolChanges(txPoolChanges);
	}

	@Override
	public Integer getTxNumber() {
		return txMempool.size();
	}

	@Override
	public boolean containsTxId(String txId) {
		// return txKeyMap.contains(txId);//This is death!! it refers to the value not
		// the key!!!
		return txMempool.containsKey(txId);
	}

	@Override
	public Optional<Transaction> getTx(String txId) {
		return Optional.ofNullable(txMempool.get(txId));
	}

	@Override
	public void drop() {
		txMempool = new ConcurrentHashMap<>();
	}

	private void logTxPoolChanges(TxPoolChanges txpc) {
		StringBuilder sb = new StringBuilder();
		sb.append("TxPoolChanges(");
		sb.append(txpc.getChangeCounter());
		sb.append("): ");
		sb.append(txpc.getNewTxs().size());
		sb.append(" new transactions, ");
		sb.append(txpc.getRemovedTxsId().size());
		sb.append(" removed transactions, ");
		sb.append(txpc.getTxAncestryChangesMap().size());
		sb.append(" updated transactions.");
		logger.info(sb.toString());
	}

}
