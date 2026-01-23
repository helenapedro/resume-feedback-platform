package com.pedro.resumeworker.ai.repository;

import com.pedro.resumeworker.ai.domain.AiFeedbackRef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiFeedbackRefRepository extends JpaRepository<AiFeedbackRef, UUID> {
    Optional<AiFeedbackRef> findTopByResumeVersion_IdOrderByFeedbackVersionDesc(UUID resumeVersionId);
}
