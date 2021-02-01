package com.mempoolrecorder.entities.database;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mempoolrecorder.bitcoindadapter.entities.blockchain.Block;
import com.mempoolrecorder.bitcoindadapter.entities.mempool.TxAncestryChanges;
import com.mempoolrecorder.entities.BlockTemplate;
import com.mempoolrecorder.utils.SysProps;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document(collection = "sonb")
public class StateOnNewBlock {
	@Id
	private Integer height;
	private Block block;
	private List<String> blockTxIds; // Block TxIds in connected or disconnected Blocks
	private Set<String> memPool = new HashSet<>();
	private BlockTemplate blockTemplate;
	private Map<String, TxAncestryChanges> txAncestryChangesMap = new HashMap<>(SysProps.EXPECTED_MAX_ANCESTRY_CHANGES);
}
