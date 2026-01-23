package com.pedro.resumeapi.ai.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface AiFeedbackMongoRepository extends MongoRepository<AiFeedbackDocument, String> {
}
