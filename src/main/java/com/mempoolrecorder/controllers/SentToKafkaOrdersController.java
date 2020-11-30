package com.mempoolrecorder.controllers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;

import com.mempoolrecorder.bitcoindadapter.entities.Transaction;
import com.mempoolrecorder.bitcoindadapter.entities.blocktemplate.BlockTemplateChanges;
import com.mempoolrecorder.bitcoindadapter.entities.mempool.TxAncestryChanges;
import com.mempoolrecorder.bitcoindadapter.entities.mempool.TxPoolChanges;
import com.mempoolrecorder.controllers.entities.RangeExecutionInfo;
import com.mempoolrecorder.controllers.errors.ErrorDetails;
import com.mempoolrecorder.controllers.exceptions.BlockNotFoundException;
import com.mempoolrecorder.controllers.exceptions.TransactionNotFoundException;
import com.mempoolrecorder.entities.database.StateOnNewBlock;
import com.mempoolrecorder.events.MempoolEvent;
import com.mempoolrecorder.events.sources.TxSource;
import com.mempoolrecorder.repositories.SonbRepository;
import com.mempoolrecorder.repositories.TxRepository;
import com.mempoolrecorder.utils.PercentLog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/kafkaOrders")
public class SentToKafkaOrdersController {

	@Autowired
	private TxRepository txRepository;

	@Autowired
	private SonbRepository sonbRepository;

	@Autowired
	private TxSource txSource;

	private static final String BLOCK_MSG = "block with height:";
	private static final String NOT_FOUND = ", not found.";

	@GetMapping("/hasBlock/{height}")
	public Boolean hasBlock(@PathVariable("height") Integer height) {
		return sonbRepository.existsById(height);
	}

	// This shoud be a post
	@GetMapping("/sendStateOnBlock/{height}")
	public void sendStateOnBlock(@PathVariable("height") Integer height)
			throws BlockNotFoundException, TransactionNotFoundException {

		Optional<StateOnNewBlock> opSonb = sonbRepository.findById(height);
		if (opSonb.isEmpty()) {
			throw new BlockNotFoundException(BLOCK_MSG + height + NOT_FOUND);
		}
		StateOnNewBlock sonb = opSonb.get();

		// First we send full mempool
		sendFullMemPool(sonb);

		// Then we send the Block
		txSource.publishMemPoolEvent(MempoolEvent.createFrom(sonb.getBlock(), 0));

	}

	// This shoud be a post
	@GetMapping("/sendRangeStateOnBlock/{initHeight}/{endHeight}")
	public RangeExecutionInfo sendRangeStateOnBlock(@PathVariable("initHeight") Integer initHeight,
			@PathVariable("endHeight") Integer endHeight) throws TransactionNotFoundException {
		RangeExecutionInfo rei = new RangeExecutionInfo();
		if (initHeight > endHeight) {
			rei.getExecutionInfoList().add("initHeight>endHeight");
			return rei;
		}

		int currHeight = initHeight;
		int plIndex = 0;
		PercentLog pl = new PercentLog(endHeight - initHeight);
		while (currHeight <= endHeight) {
			Optional<StateOnNewBlock> opSonb = sonbRepository.findById(currHeight);
			if (opSonb.isEmpty()) {
				rei.getExecutionInfoList().add(BLOCK_MSG + currHeight + NOT_FOUND);
				log.info(BLOCK_MSG + currHeight + NOT_FOUND);
			} else {
				StateOnNewBlock sonb = opSonb.get();
				// First we send full mempool
				sendFullMemPool(sonb);
				// Then we send the Block
				txSource.publishMemPoolEvent(MempoolEvent.createFrom(sonb.getBlock(), 0));
				rei.getExecutionInfoList().add(BLOCK_MSG + currHeight + ", sent.");
				log.info(BLOCK_MSG + currHeight + ", sent.");
			}
			currHeight++;
			pl.update(plIndex++, percent -> log.info("SEND_RANGE_STATE_ON_BLOCK... {}", percent));
		}
		return rei;
	}

	private void sendFullMemPool(StateOnNewBlock sonb) throws TransactionNotFoundException {
		TxPoolChanges txpc = new TxPoolChanges();

		PercentLog pl = new PercentLog(sonb.getMemPool().size());
		int counter = 0;
		Iterator<String> txIdIt = sonb.getMemPool().iterator();
		while (txIdIt.hasNext()) {
			String txId = txIdIt.next();
			Optional<Transaction> opTx = txRepository.findById(txId);
			if (opTx.isEmpty()) {
				throw new TransactionNotFoundException(
						"Error while retreiving txId:" + txId + " from database. Not found.");
			}
			Transaction tx = opTx.get();
			TxAncestryChanges txAncestryChanges = sonb.getTxAncestryChangesMap().get(tx.getTxId());
			tx.setFees(txAncestryChanges.getFees());
			tx.setTxAncestry(txAncestryChanges.getTxAncestry());

			if (txpc.getNewTxs().size() == 10) {
				Optional<BlockTemplateChanges> opBTC = Optional.empty();
				if (!txIdIt.hasNext()) {
					// This is not needed since txMempool forces a mempoolRefresh when a block
					// arrives
					// txpc.setChangeCounter(1);// Force liveMempoolRefresh in txMempool
					opBTC = createBTCFrom(sonb);// Send blockTemplate
				}
				txSource.publishMemPoolEvent(MempoolEvent.createFrom(txpc, opBTC, 0));
				txpc.setNewTxs(new ArrayList<>(10));
				pl.update(counter, percent -> log.info("Sending txMemPool for StateOnNewBlock:{} ....{}",
						sonb.getHeight(), percent));
			}
			txpc.getNewTxs().add(tx);
			counter++;
		}

		if (!txpc.getNewTxs().isEmpty()) {
			// This is not needed since txMempool forces a mempoolRefresh when a block
			// arrives
			// txpc.setChangeCounter(1);// Force liveMempoolRefresh in txMempool
			txSource.publishMemPoolEvent(MempoolEvent.createFrom(txpc, createBTCFrom(sonb), 0));// Send blockTemplate
			pl.update(counter,
					percent -> log.info("Sending txMemPool for StateOnNewBlock:{} ....{}", sonb.getHeight(), percent));
		}
	}

	private Optional<BlockTemplateChanges> createBTCFrom(StateOnNewBlock sonb) {
		BlockTemplateChanges btc = new BlockTemplateChanges();
		sonb.getBlockTemplate().stream().forEach(btTx -> btc.getAddBTTxsList().add(btTx));
		return Optional.of(btc);
	}

	@ExceptionHandler(BlockNotFoundException.class)
	public ResponseEntity<ErrorDetails> ignoringBlockNotFound(BlockNotFoundException e) {
		ErrorDetails errorDetails = new ErrorDetails();
		errorDetails.setErrorMessage(e.getMessage());
		errorDetails.setErrorCode(HttpStatus.NOT_FOUND.toString());
		return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(TransactionNotFoundException.class)
	public ResponseEntity<ErrorDetails> onTxNotFound(TransactionNotFoundException e) {
		ErrorDetails errorDetails = new ErrorDetails();
		errorDetails.setErrorMessage(e.getMessage());
		errorDetails.setErrorCode(HttpStatus.NOT_FOUND.toString());
		return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
	}

}
