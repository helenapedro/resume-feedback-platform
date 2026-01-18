package com.pedro.resumeapi.repository;

import com.pedro.resumeapi.domain.AiFeedbackRef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiFeedbackRefRepository extends JpaRepository<AiFeedbackRef, UUID> {
    List<AiFeedbackRef> findByResumeVersion_IdOrderByFeedbackVersionDesc(UUID versionId);
}
