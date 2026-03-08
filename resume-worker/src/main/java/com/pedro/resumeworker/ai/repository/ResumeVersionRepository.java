package com.pedro.resumeworker.ai.repository;

import com.pedro.resumeworker.ai.domain.ResumeVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResumeVersionRepository extends JpaRepository<ResumeVersion, UUID> {
    Optional<ResumeVersion> findTopByResumeIdAndVersionNumberLessThanOrderByVersionNumberDesc(UUID resumeId, Integer versionNumber);
}
