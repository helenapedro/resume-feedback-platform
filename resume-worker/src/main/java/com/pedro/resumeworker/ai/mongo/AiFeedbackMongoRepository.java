package com.pedro.resumeworker.ai.mongo;

import com.pedro.common.ai.mongo.AiFeedbackDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiFeedbackMongoRepository extends MongoRepository<AiFeedbackDocument, String> {
    Optional<AiFeedbackDocument> findTopByResumeVersionIdOrderByCreatedAtDesc(UUID resumeVersionId);
}
