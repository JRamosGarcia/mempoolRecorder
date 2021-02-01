package com.mempoolrecorder.controllers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.mempoolrecorder.bitcoindadapter.entities.Transaction;
import com.mempoolrecorder.bitcoindadapter.entities.mempool.TxAncestryChanges;
import com.mempoolrecorder.controllers.entities.FullStateOnNewBlock;
import com.mempoolrecorder.controllers.errors.ErrorDetails;
import com.mempoolrecorder.controllers.exceptions.BlockNotFoundException;
import com.mempoolrecorder.controllers.exceptions.TransactionNotFoundException;
import com.mempoolrecorder.entities.database.StateOnNewBlock;
import com.mempoolrecorder.repositories.SonbRepository;
import com.mempoolrecorder.repositories.TxRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fullStateOnNewBlock")
public class FullStateOnNewBlockController {
    @Autowired
    private TxRepository txRepository;

    @Autowired
    private SonbRepository sonbRepository;

    @GetMapping("/hasBlock/{height}")
    public Boolean hasBlock(@PathVariable("height") Integer height) {
        return sonbRepository.existsById(height);
    }

    @GetMapping("/height/{height}")
    public FullStateOnNewBlock getFullSonb(@PathVariable("height") Integer height)
            throws TransactionNotFoundException, BlockNotFoundException {
        Optional<StateOnNewBlock> opSonb = sonbRepository.findById(height);
        if (opSonb.isEmpty()) {
            throw new BlockNotFoundException("block with height:" + height + " not found");
        }
        StateOnNewBlock sonb = opSonb.get();
        FullStateOnNewBlock fsonb = new FullStateOnNewBlock();
        fsonb.setBlock(sonb.getBlock());
        fsonb.setBlockTemplate(sonb.getBlockTemplate());
        fsonb.setBlockTxIds(sonb.getBlockTxIds());
        fsonb.setHeight(sonb.getHeight());
        fsonb.setMemPool(getTxsOf(sonb));
        return fsonb;
    }

    private List<Transaction> getTxsOf(StateOnNewBlock sonb) throws TransactionNotFoundException {
        List<Transaction> txList = new ArrayList<>();

        Iterator<String> it = sonb.getMemPool().iterator();
        while (it.hasNext()) {
            String txId = it.next();
            Optional<Transaction> opTx = txRepository.findById(txId);
            if (opTx.isEmpty()) {
                throw new TransactionNotFoundException(
                        "Error while retreiving txId:" + txId + " from database. Not found.");
            }
            Transaction tx = opTx.get();
            TxAncestryChanges txAncestryChanges = sonb.getTxAncestryChangesMap().get(tx.getTxId());
            if (txAncestryChanges == null) {
                throw new TransactionNotFoundException(
                        "Error while retreiving txId:" + txId + " from database. Not found.");
            }
            tx.setFees(txAncestryChanges.getFees());
            tx.setTxAncestry(txAncestryChanges.getTxAncestry());
            txList.add(tx);
        }
        return txList;
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
