package com.pedro.resumeworker.ai.repository;

import com.pedro.resumeworker.ai.domain.AiFeedbackRef;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiFeedbackRefRepository extends JpaRepository<AiFeedbackRef, UUID> {
    Optional<AiFeedbackRef> findTopByResumeVersion_IdOrderByFeedbackVersionDesc(UUID resumeVersionId);

    @Query("""
            select ref
            from AiFeedbackRef ref
            join fetch ref.resumeVersion version
            where version.resumeId = :resumeId
              and version.versionNumber < :versionNumber
            order by version.versionNumber desc, ref.feedbackVersion desc
            """)
    List<AiFeedbackRef> findLatestPreviousFeedbackRefs(
            @Param("resumeId") UUID resumeId,
            @Param("versionNumber") Integer versionNumber,
            Pageable pageable);
}
