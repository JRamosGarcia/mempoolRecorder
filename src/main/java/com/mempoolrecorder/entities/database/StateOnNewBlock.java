package com.mempoolrecorder.entities.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.mempoolrecorder.bitcoindadapter.entities.blockchain.Block;
import com.mempoolrecorder.bitcoindadapter.entities.blocktemplate.BlockTemplateTx;
import com.mempoolrecorder.bitcoindadapter.entities.mempool.TxAncestryChanges;
import com.mempoolrecorder.utils.SysProps;

@Document(collection = "sonb")
public class StateOnNewBlock {

	@Id
	private Integer height;

	private Block block;
	
	private Set<String> memPool = new HashSet<>();

	private List<BlockTemplateTx> blockTemplate = new ArrayList<>(SysProps.EXPECTED_NUM_TX_IN_BLOCK);

	private Map<String, TxAncestryChanges> txAncestryChangesMap = new HashMap<>(SysProps.EXPECTED_MAX_ANCESTRY_CHANGES);

	public Integer getHeight() {
		return height;
	}

	public void setHeight(Integer height) {
		this.height = height;
	}

	public Block getBlock() {
		return block;
	}

	public void setBlock(Block block) {
		this.block = block;
	}

	public List<BlockTemplateTx> getBlockTemplate() {
		return blockTemplate;
	}

	public void setBlockTemplate(List<BlockTemplateTx> blockTemplate) {
		this.blockTemplate = blockTemplate;
	}

	public Set<String> getMemPool() {
		return memPool;
	}

	public void setMemPool(Set<String> memPool) {
		this.memPool = memPool;
	}

	public Map<String, TxAncestryChanges> getTxAncestryChangesMap() {
		return txAncestryChangesMap;
	}

	public void setTxAncestryChangesMap(Map<String, TxAncestryChanges> txAncestryChangesMap) {
		this.txAncestryChangesMap = txAncestryChangesMap;
	}

}
