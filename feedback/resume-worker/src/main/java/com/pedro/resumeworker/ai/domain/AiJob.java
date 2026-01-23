package com.pedro.resumeworker.ai.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_jobs",
        indexes = {
                @Index(name = "idx_ai_jobs_version", columnList = "resume_version_id"),
                @Index(name = "idx_ai_jobs_status", columnList = "status")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_ai_jobs_idempotency", columnNames = "idempotency_key")
)
@Getter
@Setter
public class AiJob {

    public enum Status { PENDING, PROCESSING, DONE, FAILED }

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_version_id", nullable = false)
    private ResumeVersion resumeVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "idempotency_key", length = 80, unique = true)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "error_detail", length = 1024)
    private String errorDetail;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = Status.PENDING;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
