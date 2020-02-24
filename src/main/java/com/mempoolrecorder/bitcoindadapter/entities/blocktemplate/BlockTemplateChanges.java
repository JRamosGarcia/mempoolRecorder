package com.mempoolrecorder.bitcoindadapter.entities.blocktemplate;

import java.util.ArrayList;
import java.util.List;

public class BlockTemplateChanges {

	private List<BlockTemplateTx> addBTTxsList = new ArrayList<>();
	private List<String> removeBTTxIdsList = new ArrayList<>();

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
