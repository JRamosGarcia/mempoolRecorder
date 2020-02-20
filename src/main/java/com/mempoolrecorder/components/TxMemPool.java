package com.mempoolrecorder.components;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.mempoolrecorder.bitcoindadapter.entities.Transaction;
import com.mempoolrecorder.bitcoindadapter.entities.mempool.TxPoolChanges;

public interface TxMemPool {

	void refresh(TxPoolChanges txPoolChanges);

	Integer getTxNumber();

	Stream<Transaction> getDescendingTxStream();
	
	Set<String> getAllTxIds();

	Set<String> getAllParentsOf(Transaction tx);

	Set<String> getAllChildrenOf(Transaction tx);

	boolean containsTxId(String txId);

	Optional<Transaction> getTx(String txId);

	Stream<Transaction> getTxsAfter(Instant instant);

	void drop();

}