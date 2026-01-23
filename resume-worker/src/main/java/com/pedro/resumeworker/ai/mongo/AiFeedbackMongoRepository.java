package com.pedro.resumeworker.ai.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface AiFeedbackMongoRepository extends MongoRepository<AiFeedbackDocument, String> {
}
