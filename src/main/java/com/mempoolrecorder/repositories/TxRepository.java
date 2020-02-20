package com.mempoolrecorder.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.mempoolrecorder.bitcoindadapter.entities.Transaction;

@Repository
public interface TxRepository extends MongoRepository<Transaction, String> {

}
