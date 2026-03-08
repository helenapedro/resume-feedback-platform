package com.pedro.resumeapi.ai.domain;

import com.pedro.resumeapi.resume.domain.ResumeVersion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_progress_refs",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_progress_ref_unique",
                columnNames = {"resume_version_id", "progress_version"}
        ),
        indexes = @Index(name = "idx_progress_refs_version", columnList = "resume_version_id"))
@Getter
@Setter
public class AiProgressRef {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_version_id", nullable = false)
    private ResumeVersion resumeVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "baseline_resume_version_id", nullable = false)
    private ResumeVersion baselineResumeVersion;

    @Column(name = "progress_version", nullable = false)
    private int progressVersion = 1;

    @Column(name = "mongo_doc_id", nullable = false, length = 64)
    private String mongoDocId;

    @Column(name = "model", length = 80)
    private String model;

    @Column(name = "prompt_version", length = 40)
    private String promptVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}
