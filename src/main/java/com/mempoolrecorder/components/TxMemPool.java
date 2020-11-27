package com.mempoolrecorder.components;

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

	boolean containsTxId(String txId);

	Optional<Transaction> getTx(String txId);

	void drop();

}