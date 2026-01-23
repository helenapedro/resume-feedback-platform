package com.pedro.resumeworker.ai.repository;

import com.pedro.resumeworker.ai.domain.AiJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AiJobRepository extends JpaRepository<AiJob, UUID> {
    List<AiJob> findByStatusAndNextRetryAtBefore(AiJob.Status status, Instant retryAt);
}
