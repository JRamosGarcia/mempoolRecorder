package com.mempoolrecorder.entities;

import java.util.HashMap;
import java.util.Map;

import com.mempoolrecorder.bitcoindadapter.entities.blocktemplate.BlockTemplateTx;
import com.mempoolrecorder.utils.SysProps;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BlockTemplate {

	// Height of the block to be mined with this template.
	private int height;
	private Map<String, BlockTemplateTx> blockTemplateTxMap = new HashMap<>(SysProps.HM_INITIAL_CAPACITY_FOR_BLOCK);

	public static BlockTemplate empty() {
		return new BlockTemplate();
	}

}
