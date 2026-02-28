package com.pedro.resumeapi.accessaudit.repository;

import com.pedro.resumeapi.accessaudit.domain.AccessAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccessAuditRepository extends JpaRepository<AccessAudit, UUID> {
    void deleteByResume_Id(UUID resumeId);
}

