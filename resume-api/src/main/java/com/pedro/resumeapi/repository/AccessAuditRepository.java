package com.pedro.resumeapi.repository;

import com.pedro.resumeapi.domain.AccessAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccessAuditRepository extends JpaRepository<AccessAudit, UUID> {}

