package com.mempoolrecorder.bitcoindadapter.entities.blocktemplate;

import java.util.List;

public class BlockTemplateChanges {

	private List<BlockTemplateTx> addBTTxsList;
	private List<String> removeBTTxIdsList;

	public List<BlockTemplateTx> getAddBTTxsList() {
		return addBTTxsList;
	}

	public void setAddBTTxsList(List<BlockTemplateTx> addBTTxsList) {
		this.addBTTxsList = addBTTxsList;
	}

	public List<String> getRemoveBTTxIdsList() {
		return removeBTTxIdsList;
	}

	public void setRemoveBTTxIdsList(List<String> removeBTTxIdsList) {
		this.removeBTTxIdsList = removeBTTxIdsList;
	}

}
