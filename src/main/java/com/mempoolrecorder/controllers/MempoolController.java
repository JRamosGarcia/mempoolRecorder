package com.mempoolrecorder.controllers;

import com.mempoolrecorder.components.TxMemPool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mempool")
public class MempoolController {

    @Autowired
    private TxMemPool txMemPool;

    @GetMapping("/size")
    public int getSize() {
        return txMemPool.getTxNumber();
    }

}
