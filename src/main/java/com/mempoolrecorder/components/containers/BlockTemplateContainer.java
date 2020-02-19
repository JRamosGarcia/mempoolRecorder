package com.mempoolrecorder.components.containers;

import com.mempoolrecorder.bitcoindadapter.entities.blocktemplate.BlockTemplateChanges;
import com.mempoolrecorder.entities.BlockTemplate;

public interface BlockTemplateContainer {

	BlockTemplate getBlockTemplate();

	void setBlockTemplate(BlockTemplate bt);

	void refresh(BlockTemplateChanges btc);

	void drop();

}