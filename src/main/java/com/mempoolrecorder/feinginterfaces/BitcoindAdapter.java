package com.mempoolrecorder.feinginterfaces;

import java.util.Map;

import com.mempoolrecorder.bitcoindadapter.entities.Transaction;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("bitcoindAdapter")
public interface BitcoindAdapter {

	@GetMapping(value = "/memPool/full", consumes = "application/json")
	Map<String, Transaction> getFullMemPool();

}
