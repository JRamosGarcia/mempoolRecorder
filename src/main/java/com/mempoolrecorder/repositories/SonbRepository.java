package com.mempoolrecorder.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.mempoolrecorder.entities.database.StateOnNewBlock;

@Repository
public interface SonbRepository extends MongoRepository<StateOnNewBlock, Integer> {


}
