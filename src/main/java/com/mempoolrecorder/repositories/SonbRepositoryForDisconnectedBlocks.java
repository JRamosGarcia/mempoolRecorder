package com.mempoolrecorder.repositories;

import com.mempoolrecorder.entities.database.StateOnNewBlock;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SonbRepositoryForDisconnectedBlocks extends MongoRepository<StateOnNewBlock, Integer> {

}
