package com.mempoolrecorder.threads;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mempoolrecorder.bitcoindadapter.entities.Transaction;
import com.mempoolrecorder.bitcoindadapter.entities.blockchain.Block;
import com.mempoolrecorder.bitcoindadapter.entities.mempool.TxAncestryChanges;
import com.mempoolrecorder.bitcoindadapter.entities.mempool.TxPoolChanges;
import com.mempoolrecorder.components.TxMemPool;
import com.mempoolrecorder.components.alarms.AlarmLogger;
import com.mempoolrecorder.components.containers.MempoolEventQueueContainer;
import com.mempoolrecorder.components.health.MempoolSyncedHealthIndicator;
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
    private BitcoindAdapter bitcoindAdapter;

    @Autowired
    private TxRepository txRepository;

    @Autowired
    private SonbRepository sonbRepository;

    @Autowired
    private SonbRepositoryForDisconnectedBlocks sonbRepositoryForDisconnectedBlocks;

    @Autowired
    private MempoolSyncedHealthIndicator mempoolSyncedHealthIndicator;

    private boolean isStarting = true;
    private int lastBASequence = -1;// Last BitcoindAdapter Sequence

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
        // loop until shutdown
        while (!endThread) {
            try {
                MempoolEvent event = mempoolEventQueueContainer.getBlockingQueue().take();
                log.debug("This is the event: {}", event);
                onEvent(event);
            } catch (InterruptedException e) {
                log.info("Thread interrupted for shutdown.");
                log.debug("", e);
                Thread.currentThread().interrupt();// It doesn't care, just to avoid sonar complaining.
            } catch (Exception e) {
                log.error("", e);
                alarmLogger.addAlarm("Fatal error" + ExceptionUtils.getStackTrace(e));
            }
        }
    }

    private void onEvent(MempoolEvent event) throws InterruptedException {
        if (isStarting) {
            // ResetContainers or Queries full mempool with mempoolSequence number.
            onEventonStarting(event);
            isStarting = false;
        }
        treatEvent(event);
    }

    private void onEventonStarting(MempoolEvent event) throws InterruptedException {
        if (event.getSeqNumber() == 0) {
            log.info("BitcoindAdapter is starting while we are already up");
            // We don't need resetContainers in case of bitcoindAdapter crash,
            // onErrorInSequence has done it for us.
        } else {
            log.info("BitcoindAdapater is already working, asking for full mempool and mempoolSequence...");

            Map<String, Transaction> fullMemPoolMap = null;
            while (fullMemPoolMap == null) {
                try {
                    fullMemPoolMap = bitcoindAdapter.getFullMemPool();
                } catch (Exception e) {
                    log.warn("BitcoindAdapter is not ready yet waiting 5 seconds...");
                    alarmLogger.addAlarm("BitcoindAdapter is not ready yet waiting 5 seconds...");
                    Thread.sleep(5000);
                }
            }
            TxPoolChanges txpc = new TxPoolChanges();
            txpc.setNewTxs(new ArrayList<>(fullMemPoolMap.values()));
            txMemPool.refresh(txpc);
            log.info("Full mempool has been queried from bitcoindAdapter.");
        }
        // Fake a lastBASequence because we are starting
        lastBASequence = event.getSeqNumber() - 1;
        mempoolSyncedHealthIndicator.setMempoolSynced(true);
    }

    private void treatEvent(MempoolEvent event) throws InterruptedException {
        if (errorInSeqNumber(event)) {
            onErrorInSeqNumber(event);// Makes a full reset
            return;
        }
        switch (event.getEventType()) {
            case NEW_BLOCK:
                // Block event has new block and refresh info, order matters, since we don't
                // want to update blockTemplate before treating the new block
                onNewBlock(event);
                onRefreshEvent(event);
                break;
            case REFRESH_POOL:
                onRefreshEvent(event);
                break;
            default:
                throw new IllegalArgumentException("WTF! MempoolEventType not valid");
        }
    }

    private boolean errorInSeqNumber(MempoolEvent event) {
        return ((++lastBASequence) != event.getSeqNumber());
    }

    private void onErrorInSeqNumber(MempoolEvent event) throws InterruptedException {
        // Somehow we have lost mempool events (Kafka guarrantees this not to happen).
        // We have to re-start again.
        log.error("We have lost a bitcoindAdapter MempoolEvent, sequence not expected: {}, "
                + "Reset and waiting for new full mempool and mempoolSequence...", event.getSeqNumber());
        fullReset();
        // Event is reintroduced and it's not lost never. This is a must when
        // bitcoindAdapter re-starts and send a sequenceEvent = 0
        onEvent(event);
    }

    private void fullReset() {
        resetContainers();
        // Reset downstream counter to provoke cascade resets.
        isStarting = true;
        lastBASequence = -1;// Last BitcoindAdapter Sequence
        mempoolSyncedHealthIndicator.setMempoolSynced(false);
    }

    private void onNewBlock(MempoolEvent blockEvent) {

        Optional<Block> opBlock = blockEvent.getBlock();
        if (opBlock.isEmpty()) {
            alarmLogger.addAlarm("An empty block has come in a MempoolEvent.EventType.NEW_BLOCK");
            return;// numBlocksBeforeBlockTC is not incremented (below)
        }

        Block block = blockEvent.getBlock().orElseThrow();
        List<String> blockTxIds = blockEvent.tryGetBlockTxIds().orElseThrow();
        log.info("New block(connected: {}, height: {}, hash: {}, txNum: {}) ---------------------------",
                block.getConnected(), block.getHeight(), block.getHash(), blockTxIds.size());
        StateOnNewBlock sonb = new StateOnNewBlock();
        sonb.setHeight(block.getHeight());
        sonb.setBlock(block);
        sonb.setBlockTxIds(blockTxIds);//Correct for connected and disconnected blocks

        // If not, block Template is empty, because txMempool would not had received
        // this data from bitcoind
        sonb.setBlockTemplate(blockEvent.getBlockTemplate().orElse(null));
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

    private void onRefreshEvent(MempoolEvent refreshEvent) {
        TxPoolChanges txpc = refreshEvent.getTxPoolChanges();
        validate(txpc);
        txMemPool.refresh(txpc);
    }

    private void resetContainers() {
        txMemPool.drop();
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
