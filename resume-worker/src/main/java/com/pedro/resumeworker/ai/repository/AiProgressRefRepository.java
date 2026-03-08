package com.pedro.resumeworker.ai.repository;

import com.pedro.resumeworker.ai.domain.AiProgressRef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiProgressRefRepository extends JpaRepository<AiProgressRef, UUID> {
    Optional<AiProgressRef> findTopByResumeVersion_IdOrderByProgressVersionDesc(UUID versionId);
}
