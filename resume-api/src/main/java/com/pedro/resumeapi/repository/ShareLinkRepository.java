package com.pedro.resumeapi.repository;

import com.pedro.resumeapi.domain.AccessAudit;
import com.pedro.resumeapi.domain.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShareLinkRepository extends JpaRepository<ShareLink, UUID> {
    Optional<ShareLink> findByTokenHash(String tokenHash);
    List<ShareLink> findByResume_IdOrderByCreatedAtDesc(UUID resumeId);
}
