package com.pedro.resumeapi.ai.repository;

import com.pedro.resumeapi.ai.domain.AiJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiJobRepository extends JpaRepository<AiJob, UUID> {
    Optional<AiJob> findTopByResumeVersionIdOrderByCreatedAtDesc(UUID resumeVersionId);
}
