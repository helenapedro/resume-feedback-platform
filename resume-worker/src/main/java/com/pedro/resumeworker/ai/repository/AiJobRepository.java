package com.pedro.resumeworker.ai.repository;

import com.pedro.resumeworker.ai.domain.AiJob;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AiJobRepository extends JpaRepository<AiJob, UUID> {
    @EntityGraph(attributePaths = "resumeVersion")
    List<AiJob> findTop50ByStatusOrderByCreatedAtAsc(AiJob.Status status);

    @EntityGraph(attributePaths = "resumeVersion")
    List<AiJob> findTop50ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(AiJob.Status status, Instant retryAt);

}
