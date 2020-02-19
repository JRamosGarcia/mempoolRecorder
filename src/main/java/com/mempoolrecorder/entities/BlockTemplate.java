package com.mempoolrecorder.entities;

import java.util.HashMap;
import java.util.Map;

import com.mempoolrecorder.bitcoindadapter.entities.blocktemplate.BlockTemplateTx;
import com.mempoolrecorder.utils.SysProps;

public class BlockTemplate {

	private Map<String, BlockTemplateTx> blockTemplateTxMap = new HashMap<>(SysProps.HM_INITIAL_CAPACITY_FOR_BLOCK);

	public static BlockTemplate empty() {
		return new BlockTemplate();
	}

	public Map<String, BlockTemplateTx> getBlockTemplateTxMap() {
		return blockTemplateTxMap;
	}

	public void setBlockTemplateTxMap(Map<String, BlockTemplateTx> blockTemplateTxMap) {
		this.blockTemplateTxMap = blockTemplateTxMap;
	}

}
