package com.mempoolrecorder.components.containers;

import java.util.concurrent.atomic.AtomicReference;

import com.mempoolrecorder.bitcoindadapter.entities.blocktemplate.BlockTemplateChanges;
import com.mempoolrecorder.components.alarms.AlarmLogger;
import com.mempoolrecorder.entities.BlockTemplate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BlockTemplateContainerImpl implements BlockTemplateContainer {

	@Autowired
	AlarmLogger alarmLogger;

	private AtomicReference<BlockTemplate> atomicBlockTemplate = new AtomicReference<>(BlockTemplate.empty());

	@Override
	public BlockTemplate getBlockTemplate() {
		return atomicBlockTemplate.get();
	}

	@Override
	public void setBlockTemplate(BlockTemplate bt) {
		atomicBlockTemplate.set(bt);
	}

	@Override
	public void refresh(BlockTemplateChanges btc) {

		BlockTemplate bt = getBlockTemplate();

		btc.getAddBTTxsList().forEach(btTx -> bt.getBlockTemplateTxMap().put(btTx.getTxId(), btTx));
		btc.getRemoveBTTxIdsList().forEach(btTxId -> {
			if (null == bt.getBlockTemplateTxMap().remove(btTxId)) {
				log.error("BlockTemplate does not have txId: {}", btTxId);
				alarmLogger.addAlarm("BlockTemplate does not have txId: " + btTxId);
			}
		});
		setBlockTemplate(bt);

		log.debug("new BlockTemplate(size: {} new: {} remove: {})", bt.getBlockTemplateTxMap().size(),
				btc.getAddBTTxsList().size(), btc.getRemoveBTTxIdsList().size());
	}

	@Override
	public void drop() {
		setBlockTemplate(BlockTemplate.empty());
	}

}
