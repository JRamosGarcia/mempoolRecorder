package com.mempoolrecorder.threads;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mempoolrecorder.MempoolRecorderApplication;
import com.mempoolrecorder.bitcoindadapter.entities.Transaction;
import com.mempoolrecorder.bitcoindadapter.entities.blockchain.Block;
import com.mempoolrecorder.bitcoindadapter.entities.blocktemplate.BlockTemplateChanges;
import com.mempoolrecorder.bitcoindadapter.entities.mempool.TxAncestryChanges;
import com.mempoolrecorder.bitcoindadapter.entities.mempool.TxPoolChanges;
import com.mempoolrecorder.components.TxMemPool;
import com.mempoolrecorder.components.alarms.AlarmLogger;
import com.mempoolrecorder.components.containers.BlockTemplateContainer;
import com.mempoolrecorder.components.containers.MempoolEventQueueContainer;
import com.mempoolrecorder.entities.BlockTemplate;
import com.mempoolrecorder.entities.database.StateOnNewBlock;
import com.mempoolrecorder.events.MempoolEvent;
import com.mempoolrecorder.feinginterfaces.BitcoindAdapter;
import com.mempoolrecorder.repositories.SonbRepository;
import com.mempoolrecorder.repositories.SonbRepositoryForDisconnectedBlocks;
import com.mempoolrecorder.repositories.TxRepository;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MempoolEventConsumer implements Runnable {

    @Autowired
    private MempoolEventQueueContainer mempoolEventQueueContainer;

    private boolean threadStarted = false;
    private boolean threadFinished = false;
    private Thread thread = null;
    protected boolean endThread;

    @Autowired
    protected AlarmLogger alarmLogger;

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

    @Autowired
    private SonbRepositoryForDisconnectedBlocks sonbRepositoryForDisconnectedBlocks;

    private int numBlocksBeforeBlockTC = 0;// Number of consecutive Blocks before a blockTemplateChanges arrives.

    public void start() {
        if (threadFinished)
            throw new IllegalStateException("This class only accepts only one start");

        thread = new Thread(this);
        thread.start();
        threadStarted = true;
    }

    public void shutdown() {
        if (!threadStarted)
            throw new IllegalStateException("This class is not started yet!");
        endThread = true;
        thread.interrupt();// In case thread is waiting for something.
        threadFinished = true;
    }

    @Override
    public void run() {
        try {
            while (!endThread) {
                doIt();
            }
        } catch (RuntimeException e) {
            log.error("", e);
            alarmLogger.addAlarm("Fatal error" + ExceptionUtils.getStackTrace(e));
        } catch (InterruptedException e) {
            log.info("Thread interrupted for shutdown.");
            log.debug("", e);
            Thread.currentThread().interrupt();// It doesn't care, just to avoid sonar complaining.
        }

    }

    private void doIt() throws InterruptedException {
        MempoolEvent mempoolEvent = mempoolEventQueueContainer.getBlockingQueue().take();
        if ((mempoolEvent.getEventType() == MempoolEvent.EventType.NEW_BLOCK)) {
            Optional<Block> opBlock = mempoolEvent.tryGetBlock();
            if (opBlock.isEmpty()) {
                alarmLogger.addAlarm("An empty block has come in a MempoolEvent.EventType.NEW_BLOCK");
                return;
            }
            Block block = opBlock.get();
            log.info("New block(connected: {}, height: {}, hash: {}, txNum: {}) ---------------------------",
                    block.getConnected(), block.getHeight(), block.getHash(), block.getTxIds().size());
            onNewBlock(block);
            numBlocksBeforeBlockTC++;
        } else if (mempoolEvent.getEventType() == MempoolEvent.EventType.REFRESH_POOL) {
            Optional<TxPoolChanges> opTxPC = mempoolEvent.tryGetTxPoolChanges();
            if (opTxPC.isEmpty()) {
                alarmLogger.addAlarm("An empty TxPoolChanges has come in a MempoolEvent.EventType.REFRESH_POOL");
                return;
            }
            TxPoolChanges txpc = opTxPC.get();
            Optional<BlockTemplateChanges> opBTC = mempoolEvent.tryGetBlockTemplateChanges();
            if (opBTC.isPresent()) {
                numBlocksBeforeBlockTC = 0;
            }
            validate(txpc);
            onRefreshMempoolAndBlockTemplateEvent(txpc, opBTC);
        }
    }

    private void onNewBlock(Block block) {
        log.info("New block with height: {}, numBlocksBeforeBlockTC: {}", block.getHeight(), numBlocksBeforeBlockTC);
        StateOnNewBlock sonb = new StateOnNewBlock();
        sonb.setHeight(block.getHeight());
        sonb.setBlock(block);
        if (numBlocksBeforeBlockTC == 0) {
            // If not, block Template is empty, because txMempool would not had received
            // this data from bitcoind
            sonb.setBlockTemplate(blockTemplateContainer.getBlockTemplate().getBlockTemplateTxMap().values().stream()
                    .collect(Collectors.toList()));
        }
        // Not sure if this is useful for disconected blocks
        txMemPool.getDescendingTxStream().forEach(tx -> {
            sonb.getMemPool().add(tx.getTxId());
            sonb.getTxAncestryChangesMap().put(tx.getTxId(), new TxAncestryChanges(tx.getFees(), tx.getTxAncestry()));
            if (!txRepository.existsById(tx.getTxId())) {
                txRepository.save(tx);
            }
        });
        if (block.getConnected().equals(Block.CONNECTED_BLOCK)) {
            // A connected block overwrite the latter
            sonbRepository.save(sonb);
        } else {
            // disconnected blocks goes to other repository.
            sonbRepositoryForDisconnectedBlocks.save(sonb);
        }
    }

    // If txpc.changeCounter == 0 this means a bitcoindAdapter reset, we must reset.
    // else Refresh mempool and blockTemplateContainer
    private void onRefreshMempoolAndBlockTemplateEvent(TxPoolChanges txpc, Optional<BlockTemplateChanges> opBTC) {
        if (txpc.getChangeCounter() == 0) {
            log.info("Receiving full txMemPool due to bitcoindAdapter/txMemPool (re)start. "
                    + "Dropping last txMemPool and BlockTemplate (if any) It can take a while...");
            dropContainers();
            reloadContainers();
            log.info("Full txMemPool and blockTemplate received!");
        } else {
            txMemPool.refresh(txpc);
            log.info("{} transactions in txMemPool.", txMemPool.getTxNumber());
            opBTC.ifPresent(blockTemplateContainer::refresh);
        }
    }

    private void dropContainers() {
        txMemPool.drop();
        blockTemplateContainer.drop();
    }

    // Reloads containers from bitcoindAdapter
    public void reloadContainers() {
        try {
            BlockTemplate blockTemplate = bitcoindAdapter.getBlockTemplate();
            Map<String, Transaction> fullMemPoolMap = bitcoindAdapter.getFullMemPool();

            TxPoolChanges txpc = new TxPoolChanges();
            txpc.setChangeCounter(0);// Force reset
            txpc.setChangeTime(Instant.now());
            txpc.setNewTxs(new ArrayList<>(fullMemPoolMap.values()));
            txMemPool.refresh(txpc);
            blockTemplateContainer.setBlockTemplate(blockTemplate);

        } catch (Exception e) {
            // When loading if there are no clients, shutdown.
            log.error(e.toString());
            alarmLogger.addAlarm("Eror en MemPoolEventsHandler.run, stopping txMemPool service: " + e.toString());
            MempoolRecorderApplication.exit();
        }
    }

    private void validate(TxPoolChanges txpc) {
        txpc.getNewTxs().stream().forEach(this::validateTx);
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
                Validate.notNull(input.getVOutIndex(), "input.voutIndex can't be null");
                Validate.notNull(input.getAmount(), "input.amount can't be null");
                // Input address could be null in case of unrecognized input scripts
            }
        });

        tx.getTxOutputs().forEach(output -> {
            // addressIds can be null if script is not recognized.
            Validate.notNull(output.getAmount(), "amount can't be null in a TxOutput");
            Validate.notNull(output.getIndex(), "index can't be null in a TxOutput");
        });
    }

}
