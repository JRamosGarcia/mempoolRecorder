package com.mempoolrecorder.bitcoindadapter.entities;

import java.util.ArrayList;
import java.util.List;

public class TxAncestry {
	private Integer descendantCount;// The number of in-mempool descendant transactions (including this one)
	private Integer descendantSize;// virtual transaction size of in-mempool descendants (including this one)
	private Integer ancestorCount;// The number of in-mempool ancestor transactions (including this one)
	private Integer ancestorSize;// virtual transaction size of in-mempool ancestors (including this one)
	private List<String> depends = new ArrayList<>();// unconfirmed transactions used as inputs for this transaction
														// (txIds list)
	private List<String> spentby = new ArrayList<>();// unconfirmed transactions spending outputs from this transaction
														// (txIds list)

	public TxAncestry() {

	}

	public Integer getDescendantCount() {
		return descendantCount;
	}

	public void setDescendantCount(Integer descendantCount) {
		this.descendantCount = descendantCount;
	}

	public Integer getDescendantSize() {
		return descendantSize;
	}

	public void setDescendantSize(Integer descendantSize) {
		this.descendantSize = descendantSize;
	}

	public Integer getAncestorCount() {
		return ancestorCount;
	}

	public void setAncestorCount(Integer ancestorCount) {
		this.ancestorCount = ancestorCount;
	}

	public Integer getAncestorSize() {
		return ancestorSize;
	}

	public void setAncestorSize(Integer ancestorSize) {
		this.ancestorSize = ancestorSize;
	}

	public List<String> getDepends() {
		return depends;
	}

	public void setDepends(List<String> depends) {
		this.depends = depends;
	}

	public List<String> getSpentby() {
		return spentby;
	}

	public void setSpentby(List<String> spentby) {
		this.spentby = spentby;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TxAncestry [descendantCount=");
		builder.append(descendantCount);
		builder.append(", descendantSize=");
		builder.append(descendantSize);
		builder.append(", ancestorCount=");
		builder.append(ancestorCount);
		builder.append(", ancestorSize=");
		builder.append(ancestorSize);
		builder.append(", depends=");
		builder.append(depends);
		builder.append(", spentby=");
		builder.append(spentby);
		builder.append("]");
		return builder.toString();
	}

}
