package com.mempoolrecorder.events.sinks;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.ApplicationListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.kafka.event.ListenerContainerIdleEvent;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;

import com.mempoolrecorder.MempoolRecorderApplication;
import com.mempoolrecorder.bitcoindadapter.entities.Transaction;
import com.mempoolrecorder.bitcoindadapter.entities.blockchain.Block;
import com.mempoolrecorder.bitcoindadapter.entities.blocktemplate.BlockTemplateChanges;
import com.mempoolrecorder.bitcoindadapter.entities.mempool.TxAncestryChanges;
import com.mempoolrecorder.bitcoindadapter.entities.mempool.TxPoolChanges;
import com.mempoolrecorder.components.TxMemPool;
import com.mempoolrecorder.components.alarms.AlarmLogger;
import com.mempoolrecorder.components.containers.BlockTemplateContainer;
import com.mempoolrecorder.entities.BlockTemplate;
import com.mempoolrecorder.entities.database.StateOnNewBlock;
import com.mempoolrecorder.events.CustomChannels;
import com.mempoolrecorder.events.MempoolEvent;
import com.mempoolrecorder.feinginterfaces.BitcoindAdapter;
import com.mempoolrecorder.repositories.SonbRepository;
import com.mempoolrecorder.repositories.TxRepository;

@EnableBinding(CustomChannels.class)
public class TxMemPoolEventsHandler implements Runnable, ApplicationListener<ListenerContainerIdleEvent> {

	@Autowired
	private AlarmLogger alarmLogger;

	@Autowired
	private TaskExecutor taskExecutor;

	@Autowired
	private TxMemPool txMemPool;

	@Autowired
	private BlockTemplateContainer blockTemplateContainer;

	@Autowired
	private BitcoindAdapter bitcoindAdapter;

	@Autowired
	private TxRepository txRepository;

	@Autowired
	private SonbRepository sonbRepository;

	@Value("${spring.cloud.stream.bindings.txMemPoolEvents.destination}")
	private String topic;

	private AtomicBoolean doResume = new AtomicBoolean(false);

	private int numConsecutiveBlocks = 0;// Number of consecutive Blocks before a refresh is made.

	private AtomicBoolean initializing = new AtomicBoolean(true);

	private AtomicBoolean loadingFullMempool = new AtomicBoolean(false);

	private boolean updateFullTxMemPool = true;

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@StreamListener("txMemPoolEvents")
	public void blockSink(MempoolEvent mempoolEvent, @Header(KafkaHeaders.CONSUMER) Consumer<?, ?> consumer) {
		try {
			if ((mempoolEvent.getEventType() == MempoolEvent.EventType.NEW_BLOCK) && (!initializing.get())) {
				Block block = mempoolEvent.tryGetBlock().get();
				logger.info("New block(height: " + block.getHeight() + ", hash:" + block.getHash() + "txNum: "
						+ block.getTxIds().size() + ") ---------------------------");
				OnNewBlock(block);
				// alarmLogger.prettyPrint();
				numConsecutiveBlocks++;
			} else if (mempoolEvent.getEventType() == MempoolEvent.EventType.REFRESH_POOL) {
				numConsecutiveBlocks = 0;
				// OnRefreshPool
				TxPoolChanges txpc = mempoolEvent.tryGetTxPoolChanges().get();
				Optional<BlockTemplateChanges> opBTC = mempoolEvent.tryGetBlockTemplateChanges();
				validate(txpc);
				// When initializing but bitcoindAdapter is not intitializing
				if ((initializing.get()) && (txpc.getChangeCounter() != 0) && (!loadingFullMempool.get())) {
					// We pause incoming messages, but several messages has been taken from kafka at
					// once so this method will be called several times. Refresh the mempool only if
					// not initializing
					logger.info("txMemPool is starting but bitcoindAdapter started long ago... "
							+ "pausing receiving kafka messages and loading full mempool from REST interface");
					consumer.pause(Collections.singleton(new TopicPartition(topic, 0)));
					loadingFullMempool.set(true);
					// Load full mempool asyncronous via REST service, then resume kafka msgs
					doFullLoadAsync();// Method must return ASAP, this is a kafka queue.
				} else if (!loadingFullMempool.get()) {// This is because consumer.pause does not pause inmediately
					refreshContainers(txpc, opBTC);
					initializing.set(false);
				}
			}
		} catch (Exception e) {
			logger.error("Exception: ", e);
			alarmLogger.addAlarm("Exception in @StreamListener of txMemPoolEvents" + e.toString());
		}
	}

	private void validate(TxPoolChanges txpc) {
		txpc.getNewTxs().stream().forEach(tx -> validateTx(tx));
	}

	private void validateTx(Transaction tx) {
		Validate.notNull(tx.getTxId(), "txId can't be null");
		Validate.notNull(tx.getTxInputs(), "txInputs can't be null");
		Validate.notNull(tx.getTxOutputs(), "txOutputs can't be null");
		Validate.notNull(tx.getWeight(), "weight can't be null");
		Validate.notNull(tx.getFees(), "Fees object can't be null");
		Validate.notNull(tx.getFees().getBase(), "Fees.base can't be null");
		Validate.notNull(tx.getFees().getModified(), "Fees.modified can't be null");
		Validate.notNull(tx.getFees().getAncestor(), "Fees.ancestor can't be null");
		Validate.notNull(tx.getFees().getDescendant(), "Fees.descendant can't be null");
		Validate.notNull(tx.getTimeInSecs(), "timeInSecs can't be null");
		Validate.notNull(tx.getTxAncestry(), "txAncestry can't be null");
		Validate.notNull(tx.getTxAncestry().getDescendantCount(), "descendantCount can't be null");
		Validate.notNull(tx.getTxAncestry().getDescendantSize(), "descendantSize can't be null");
		Validate.notNull(tx.getTxAncestry().getAncestorCount(), "ancestorCount can't be null");
		Validate.notNull(tx.getTxAncestry().getAncestorSize(), "ancestorSize can't be null");
		Validate.notNull(tx.getTxAncestry().getDepends(), "depends can't be null");
		Validate.notNull(tx.getBip125Replaceable(), "bip125Replaceable can't be null");
		Validate.notEmpty(tx.getHex(), "Hex can't be empty");

		tx.getTxInputs().forEach(input -> {
			if (input.getCoinbase() == null) {
				Validate.notNull(input.getTxId(), "input.txId can't be null");
				Validate.notNull(input.getvOutIndex(), "input.voutIndex can't be null");
				Validate.notNull(input.getAmount(), "input.amount can't be null");
				// Input address could be null in case of unrecognized input scripts
				// Validate.notNull(input.getAddressIds());
			}
		});

		tx.getTxOutputs().forEach(output -> {
			// addressIds can be null if script is not recognized.
			Validate.notNull(output.getAmount(), "amount can't be null in a TxOutput");
			Validate.notNull(output.getIndex(), "index can't be null in a TxOutput");
		});

	}

	private void OnNewBlock(Block block) {

		logger.info("New block with height: {}, numConsecutiveBlocks: {}", block.getHeight(), numConsecutiveBlocks);

		StateOnNewBlock sonb = new StateOnNewBlock();

		sonb.setHeight(block.getHeight());
		sonb.setBlock(block);
		if (numConsecutiveBlocks == 0) {
			// If not, block Template is empty, because txMempool would not had received
			// this data from bitcoind
			sonb.setBlockTemplate(blockTemplateContainer.getBlockTemplate().getBlockTemplateTxMap().values().stream()
					.collect(Collectors.toList()));
		}

		txMemPool.getDescendingTxStream().forEach(tx -> {
			sonb.getMemPool().add(tx.getTxId());
			sonb.getTxAncestryChangesMap().put(tx.getTxId(), new TxAncestryChanges(tx.getFees(), tx.getTxAncestry()));
			if (!txRepository.existsById(tx.getTxId())) {
				txRepository.save(tx);
			}
		});

		// To emulate the data txMempool would have had
		substractAlreadyMinedToSonb(sonb);
		sonbRepository.save(sonb);
	}

	private void substractAlreadyMinedToSonb(StateOnNewBlock sonb) {
		int count = numConsecutiveBlocks;
		while (count > 0) {
			int blockToFind = sonb.getHeight() - count;
			Optional<StateOnNewBlock> opSonbBD = sonbRepository.findById(blockToFind);
			if (opSonbBD.isEmpty()) {
				alarmLogger.addAlarm("Block: " + blockToFind + " is not found in db");
			} else {
				StateOnNewBlock sonbBD = opSonbBD.get();
				sonbBD.getBlock().getTxIds().forEach(txId -> {
					sonb.getMemPool().remove(txId);
					sonb.getTxAncestryChangesMap().remove(txId);
				});
			}
			count--;
		}
	}

	// Refresh mempool, liveMiningQueue, blockTemplateContainer and
	// liveAlgorithmDiffContainer
	private void refreshContainers(TxPoolChanges txpc, Optional<BlockTemplateChanges> opBTC) {
		// Order of this operations matters.
		refreshMempool(txpc);
		opBTC.ifPresent(blockTemplateContainer::refresh);
	}

	public void refreshMempool(TxPoolChanges txPoolChanges) {
		if (txPoolChanges.getChangeCounter() == 0) {
			if (updateFullTxMemPool) {
				logger.info("Receiving full txMemPool due to bitcoindAdapter/txMemPool (re)start. "
						+ "Dropping last txMemPool and BlockTemplate (if any) It can take a while...");
				txMemPool.drop();
				blockTemplateContainer.drop();
				updateFullTxMemPool = false;
			}
			txMemPool.refresh(txPoolChanges);
		} else {
			if (!updateFullTxMemPool) {
				logger.info("Full txMemPool received!");
			}
			updateFullTxMemPool = true;// Needed if bitcoindAdapter restarts
			txMemPool.refresh(txPoolChanges);
			logger.info("{} transactions in txMemPool.", txMemPool.getTxNumber());
		}
	}

	private void doFullLoadAsync() {
		taskExecutor.execute(this);
	}

	// Kafka consumer is not thread-safe so we must call pause and resume in the
	// calling thread.
	@Override
	public void onApplicationEvent(ListenerContainerIdleEvent event) {
		// logger.info(event.toString());
		if (doResume.get()) {
			if (event.getConsumer().paused().size() > 0) {
				event.getConsumer().resume(event.getConsumer().paused());
			}
			doResume.set(false);
		}
	}

	@Override
	public void run() {
		try {
			BlockTemplate blockTemplate = bitcoindAdapter.getBlockTemplate();
			Map<String, Transaction> fullMemPoolMap = bitcoindAdapter.getFullMemPool();

			TxPoolChanges txpc = new TxPoolChanges();
			txpc.setChangeCounter(0);// Force reset
			txpc.setChangeTime(Instant.now());
			txpc.setNewTxs(new ArrayList<>(fullMemPoolMap.values()));
			txMemPool.refresh(txpc);
			blockTemplateContainer.setBlockTemplate(blockTemplate);

			initializing.set(false);
			loadingFullMempool.set(false);
			doResume.set(true);
		} catch (Exception e) {
			// When loading if there are no clients, shutdown.
			logger.error(e.toString());
			alarmLogger.addAlarm("Eror en MemPoolEventsHandler.run, stopping txMemPool service: " + e.toString());
			MempoolRecorderApplication.exit();
		}
	}

}
