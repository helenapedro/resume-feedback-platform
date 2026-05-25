package com.pedro.resumeapi.ai.repository;

import com.pedro.resumeapi.ai.domain.AiFeedbackRef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiFeedbackRefRepository extends JpaRepository<AiFeedbackRef, UUID> {
    List<AiFeedbackRef> findByResumeVersion_IdOrderByFeedbackVersionDesc(UUID versionId);

    Optional<AiFeedbackRef> findByResumeVersion_IdAndFeedbackVersion(UUID versionId, int feedbackVersion);

    List<AiFeedbackRef> findByResumeVersion_Resume_Id(UUID resumeId);

    void deleteByResumeVersion_Resume_Id(UUID resumeId);
}
