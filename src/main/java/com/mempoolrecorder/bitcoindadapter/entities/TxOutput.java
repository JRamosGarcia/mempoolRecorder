package com.mempoolrecorder.bitcoindadapter.entities;

import java.util.ArrayList;
import java.util.List;

public class TxOutput {
	private List<String> addressIds = new ArrayList<>();// Several addresses if P2PSH, none if unrecognized script.
	private Long amount;// In Satoshis.
	private Integer index;// Begins in 0

	public void setAddressIds(List<String> addressIds) {
		this.addressIds = addressIds;
	}

	public void setAmount(Long amount) {
		this.amount = amount;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}

	public List<String> getAddressIds() {
		return addressIds;
	}

	public Long getAmount() {
		return amount;
	}

	public Integer getIndex() {
		return index;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TxOutput [addressIds=");
		builder.append(addressIds);
		builder.append(", amount=");
		builder.append(amount);
		builder.append(", index=");
		builder.append(index);
		builder.append("]");
		return builder.toString();
	}

}
