package com.mempoolrecorder.controllers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mempoolrecorder.bitcoindadapter.entities.Transaction;
import com.mempoolrecorder.bitcoindadapter.entities.mempool.TxPoolChanges;
import com.mempoolrecorder.controllers.errors.ErrorDetails;
import com.mempoolrecorder.controllers.exceptions.BlockNotFoundException;
import com.mempoolrecorder.controllers.exceptions.TransactionNotFoundException;
import com.mempoolrecorder.entities.database.StateOnNewBlock;
import com.mempoolrecorder.events.MempoolEvent;
import com.mempoolrecorder.events.sources.TxSource;
import com.mempoolrecorder.repositories.SonbRepository;
import com.mempoolrecorder.repositories.TxRepository;
import com.mempoolrecorder.utils.PercentLog;

@RestController
@RequestMapping("/sendToKafkaOrder")
public class SentToKafkaOrdersController {

	private static Logger logger = LoggerFactory.getLogger(SentToKafkaOrdersController.class);

	@Autowired
	private TxRepository txRepository;

	@Autowired
	private SonbRepository sonbRepository;

	@Autowired
	private TxSource txSource;

	@PostMapping("/sendStateOnBlock/{height}")
	public void sendStateOnBlock(@PathVariable("height") Integer height)
			throws BlockNotFoundException, TransactionNotFoundException {

		Optional<StateOnNewBlock> opSonb = sonbRepository.findById(height);
		if (opSonb.isEmpty()) {
			throw new BlockNotFoundException("block with height:" + height + ", not found.");
		}
		StateOnNewBlock sonb = opSonb.get();

		sendFullMemPool(sonb);

		// Send Block
		txSource.publishMemPoolEvent(MempoolEvent.createFrom(sonb.getBlock()));
	}

	private void sendFullMemPool(StateOnNewBlock sonb) throws TransactionNotFoundException {
		TxPoolChanges txpc = new TxPoolChanges();
		txpc.setChangeCounter(0);// All change counter are set to 0
		txpc.setChangeTime(Instant.now());

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

			if (txpc.getNewTxs().size() == 10) {
				if (!txIdIt.hasNext()) {
					txpc.setChangeCounter(1);// Force liveMempoolRefresh in txMempool
				}
				txSource.publishMemPoolEvent(MempoolEvent.createFrom(txpc, Optional.empty()));
				txpc.setNewTxs(new ArrayList<>(10));
				pl.update(counter, (percent) -> logger.info("Sending full txMemPool: {}", percent));
			}
			txpc.getNewTxs().add(tx);
			counter++;
		}

		if (txpc.getNewTxs().size() != 0) {
			txpc.setChangeCounter(1);// Force liveMempoolRefresh in txMempool
			txSource.publishMemPoolEvent(MempoolEvent.createFrom(txpc, Optional.empty()));
			pl.update(counter, (percent) -> logger.info("Sending full txMemPool: {}", percent));
		}
	}

	@ExceptionHandler(BlockNotFoundException.class)
	public ResponseEntity<?> onIgnoringBlockNotFound(BlockNotFoundException e) {
		ErrorDetails errorDetails = new ErrorDetails();
		errorDetails.setErrorMessage(e.getMessage());
		errorDetails.setErrorCode(HttpStatus.NOT_FOUND.toString());
		return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(TransactionNotFoundException.class)
	public ResponseEntity<?> onTxNotFound(TransactionNotFoundException e) {
		ErrorDetails errorDetails = new ErrorDetails();
		errorDetails.setErrorMessage(e.getMessage());
		errorDetails.setErrorCode(HttpStatus.NOT_FOUND.toString());
		return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
	}

}
