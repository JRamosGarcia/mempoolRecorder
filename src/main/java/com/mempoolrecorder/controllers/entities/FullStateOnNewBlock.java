package com.mempoolrecorder.controllers.entities;

import java.util.ArrayList;
import java.util.List;

import com.mempoolrecorder.bitcoindadapter.entities.Transaction;
import com.mempoolrecorder.bitcoindadapter.entities.blockchain.Block;
import com.mempoolrecorder.entities.BlockTemplate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FullStateOnNewBlock {
    private Integer height;
    private Block block;
    private List<String> blockTxIds; // Block TxIds in connected or disconnected Blocks
    private List<Transaction> memPool = new ArrayList<>(); // Mempool as-is before block is received
    private BlockTemplate blockTemplate;// BlockTemplate as-is before block is received
}
