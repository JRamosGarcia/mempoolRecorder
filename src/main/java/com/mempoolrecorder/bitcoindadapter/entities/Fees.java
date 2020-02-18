package com.mempoolrecorder.bitcoindadapter.entities;

public class Fees {
	private Long base;
	private Long modified;
	private Long ancestor;
	private Long descendant;
	
	public Long getBase() {
		return base;
	}
	public void setBase(Long base) {
		this.base = base;
	}
	public Long getModified() {
		return modified;
	}
	public void setModified(Long modified) {
		this.modified = modified;
	}
	public Long getAncestor() {
		return ancestor;
	}
	public void setAncestor(Long ancestor) {
		this.ancestor = ancestor;
	}
	public Long getDescendant() {
		return descendant;
	}
	public void setDescendant(Long descendant) {
		this.descendant = descendant;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Fees [base=");
		builder.append(base);
		builder.append(", modified=");
		builder.append(modified);
		builder.append(", ancestor=");
		builder.append(ancestor);
		builder.append(", descendant=");
		builder.append(descendant);
		builder.append("]");
		return builder.toString();
	}

}
