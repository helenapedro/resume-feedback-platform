package com.pedro.resumeapi.ai.repository;

import com.pedro.resumeapi.ai.domain.AiProgressRef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiProgressRefRepository extends JpaRepository<AiProgressRef, UUID> {
    Optional<AiProgressRef> findTopByResumeVersion_IdOrderByProgressVersionDesc(UUID versionId);

    List<AiProgressRef> findByResumeVersion_Resume_Id(UUID resumeId);

    void deleteByResumeVersion_Resume_Id(UUID resumeId);
}
