package com.pedro.resumeapi.repository;

import com.pedro.resumeapi.domain.ResumeVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeVersionRepository extends JpaRepository<ResumeVersion, UUID> {
    List<ResumeVersion> findByResume_IdOrderByVersionNumberDesc(UUID resumeId);

    Optional<ResumeVersion> findTopByResume_IdOrderByVersionNumberDesc(UUID resumeId);

}
