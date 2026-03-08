package com.pedro.resumeapi.ai.mongo;

import com.pedro.common.ai.mongo.AiProgressDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiProgressMongoRepository extends MongoRepository<AiProgressDocument, String> {
    Optional<AiProgressDocument> findTopByResumeVersionIdOrderByCreatedAtDesc(UUID resumeVersionId);
}
