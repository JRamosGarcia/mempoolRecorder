package com.mempoolrecorder.components;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.mempoolrecorder.bitcoindadapter.entities.Transaction;
import com.mempoolrecorder.bitcoindadapter.entities.mempool.TxPoolChanges;
import com.mempoolrecorder.entities.mempool.TxKey;

@Component
public class TxMemPoolImpl implements TxMemPool {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private ConcurrentSkipListMap<TxKey, Transaction> txMemPool = new ConcurrentSkipListMap<>();

	// This is very anoying but necessary since txPoolChanges.getRemovedTxsId() are
	// Strings, not TxKeys. :-(
	private ConcurrentHashMap<String, TxKey> txKeyMap = new ConcurrentHashMap<>();

	@Override
	public void refresh(TxPoolChanges txPoolChanges) {
		txPoolChanges.getNewTxs().stream().forEach(tx -> {
			TxKey txKey = new TxKey(tx.getTxId(), tx.getSatvByteIncludingAncestors(), tx.getTimeInSecs());
			txKeyMap.put(tx.getTxId(), txKey);
			txMemPool.put(txKey, tx);
		});
		txPoolChanges.getRemovedTxsId().stream().forEach(txId -> {
			TxKey txKey = txKeyMap.remove(txId);
			if (null != txKey) {
				txMemPool.remove(txKey);
			} else {
				logger.info("Removing non existing tx from mempool, txId: {}", txId);
			}
		});
		txPoolChanges.getTxAncestryChangesMap().entrySet().stream().forEach(entry -> {
			TxKey txKey = txKeyMap.remove(entry.getKey());
			if (null == txKey) {
				logger.info("Non existing txKey in txKeyMap for update, txId: {}", entry.getKey());
				return;
			}
			Transaction oldTx = txMemPool.remove(txKey);
			if (null == oldTx) {
				logger.info("Non existing tx in txMemPool for update, txId: {}", entry.getKey());
				return;
			}
			// remove+put must be made each modification since tx modification while on map
			// is pretty unsafe. (suffered in my own skin)
			oldTx.setFees(entry.getValue().getFees());
			oldTx.setTxAncestry(entry.getValue().getTxAncestry());
			txKey = new TxKey(oldTx.getTxId(), oldTx.getSatvByteIncludingAncestors(), oldTx.getTimeInSecs());
			txKeyMap.put(oldTx.getTxId(), txKey);
			txMemPool.put(txKey, oldTx);
		});
		logTxPoolChanges(txPoolChanges);
	}

	@Override
	public Integer getTxNumber() {
		return txKeyMap.size();
	}

	@Override
	public Stream<Transaction> getDescendingTxStream() {
		return txMemPool.descendingMap().entrySet().stream().map(e -> e.getValue());
	}

	@Override
	public boolean containsTxId(String txId) {
		// return txKeyMap.contains(txId);//This is death!! it refers to the value not
		// the key!!!
		return txKeyMap.containsKey(txId);
	}

	@Override
	public Optional<Transaction> getTx(String txId) {
		TxKey txKey = txKeyMap.get(txId);
		if (txKey == null)
			return Optional.empty();
		Transaction transaction = txMemPool.get(txKey);
		return Optional.ofNullable(transaction);
	}

	@Override
	public Set<String> getAllTxIds() {
		return txKeyMap.keySet();
	}


	// TODO: must be tested
	@Override
	public Set<String> getAllParentsOf(Transaction tx) {
		// recursive witchcraft
		List<String> txDepends = tx.getTxAncestry().getDepends();
		if (!txDepends.isEmpty()) {
			Set<String> parentSet = txDepends.stream().collect(Collectors.toSet());
			Set<String> granpaSet = parentSet.stream().map(txId -> txKeyMap.get(txId)).filter(txKey -> txKey != null)
					.map(txKey -> txMemPool.get(txKey)).filter(trx -> trx != null).map(trx -> getAllParentsOf(trx))
					.flatMap(pSet -> pSet.stream()).collect(Collectors.toSet());
			parentSet.addAll(granpaSet);
			return parentSet;
		}
		return new HashSet<>();
	}

	// TODO: must be tested
	@Override
	public Set<String> getAllChildrenOf(Transaction tx) {
		// recursive witchcraft
		List<String> txSpentBys = tx.getTxAncestry().getSpentby();
		if (!txSpentBys.isEmpty()) {
			Set<String> childrenSet = txSpentBys.stream().collect(Collectors.toSet());
			Set<String> granSonsSet = childrenSet.stream().map(txId -> txKeyMap.get(txId))
					.filter(txKey -> txKey != null).map(txKey -> txMemPool.get(txKey)).filter(trx -> trx != null)
					.map(trx -> getAllChildrenOf(trx)).flatMap(pSet -> pSet.stream()).collect(Collectors.toSet());
			childrenSet.addAll(granSonsSet);
			return childrenSet;
		}
		return new HashSet<>();
	}

	@Override
	public Stream<Transaction> getTxsAfter(Instant instant) {
		return txMemPool.values().stream().filter(tx -> tx.getTimeInSecs() > instant.getEpochSecond());
	}

	@Override
	public void drop() {
		txMemPool = new ConcurrentSkipListMap<>();
		txKeyMap = new ConcurrentHashMap<>();
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
