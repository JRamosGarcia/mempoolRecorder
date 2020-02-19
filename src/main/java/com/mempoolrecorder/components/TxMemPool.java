package com.mempoolrecorder.components;

import java.util.Optional;

import com.mempoolrecorder.bitcoindadapter.entities.Transaction;
import com.mempoolrecorder.bitcoindadapter.entities.mempool.TxPoolChanges;

public interface TxMemPool {

	void refresh(TxPoolChanges txPoolChanges);

	Integer getTxNumber();

	boolean containsTxId(String txId);

	Optional<Transaction> getTx(String txId);

	void drop();

}