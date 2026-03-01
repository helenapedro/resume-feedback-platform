package com.pedro.resumeworker.ai.mongo;

import com.pedro.common.ai.mongo.AiFeedbackDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AiFeedbackMongoRepository extends MongoRepository<AiFeedbackDocument, String> {
}
