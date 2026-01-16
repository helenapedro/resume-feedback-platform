package com.pedro.resumeapi.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resume_versions",
        uniqueConstraints = @UniqueConstraint(name = "uk_resume_version", columnNames = {"resume_id", "version_number"}))
@Getter @Setter
public class ResumeVersion {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "resume_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID resumeId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "storage_key", nullable = false, length = 1024)
    private String storageKey; // for now: local file path; later: S3 key (+ versionId)

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}
