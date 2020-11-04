package com.mempoolrecorder.bitcoindadapter.entities;

import java.util.ArrayList;
import java.util.List;

public class TxInput {
	private List<String> addressIds = new ArrayList<>();// Several addresses if P2PSH, none if unrecognized script. or
	// coinBase transaction
	private Long amount;// In Satoshis.
	private String txId;// Transaction where output is being spent by this input
	private Integer vOutIndex;// The output index number (vout) of the outpoint being spent. The first output
	// in a transaction has an index of 0. Not present if this is a coinbase
	// transaction
	private String coinbase;// Coinbase, normally null.

	public List<String> getAddressIds() {
		return addressIds;
	}

	public void setAddressIds(List<String> addressIds) {
		this.addressIds = addressIds;
	}

	public void setAmount(Long amount) {
		this.amount = amount;
	}

	public void setTxId(String txId) {
		this.txId = txId;
	}

	public void setVOutIndex(Integer vOutIndex) {
		this.vOutIndex = vOutIndex;
	}

	public Long getAmount() {
		return amount;
	}

	public String getTxId() {
		return txId;
	}

	public Integer getVOutIndex() {
		return vOutIndex;
	}

	public String getCoinbase() {
		return coinbase;
	}

	public void setCoinbase(String coinbase) {
		this.coinbase = coinbase;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TxInput [addressIds=");
		builder.append(addressIds);
		builder.append(", amount=");
		builder.append(amount);
		builder.append(", txId=");
		builder.append(txId);
		builder.append(", vOutIndex=");
		builder.append(vOutIndex);
		builder.append(", coinbase=");
		builder.append(coinbase);
		builder.append("]");
		return builder.toString();
	}

}
