package com.mempoolrecorder.feinginterfaces;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.mempoolrecorder.bitcoindadapter.entities.Transaction;
import com.mempoolrecorder.entities.BlockTemplate;

@FeignClient("bitcoindAdapter")
public interface BitcoindAdapter {

	@GetMapping(value = "/memPool/full", consumes = "application/json")
	Map<String, Transaction> getFullMemPool();

	@GetMapping(value = "/blockTemplate/blockTemplate", consumes = "application/json")
	BlockTemplate getBlockTemplate();

}
