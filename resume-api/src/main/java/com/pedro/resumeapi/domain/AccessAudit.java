package com.pedro.resumeapi.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "access_audit",
        indexes = {
                @Index(name = "idx_audit_share_link", columnList = "share_link_id, occurred_at"),
                @Index(name = "idx_audit_resume", columnList = "resume_id, occurred_at")
        })
@Getter @Setter
public class AccessAudit {

    public enum EventType {
        OPEN_LINK,
        DOWNLOAD,
        DOWNLOAD_ATTEMPT,
        DOWNLOAD_GRANTED,
        DOWNLOAD_DENIED,
        COMMENT_ATTEMPT,
        COMMENT_CREATED,
        COMMENT_DENIED
    }

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "share_link_id", nullable = false)
    private ShareLink shareLink;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_version_id")
    private ResumeVersion resumeVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private EventType eventType;

    @Column(name = "ip_address", length = 60)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "success", nullable = false)
    private boolean success = true;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (occurredAt == null) occurredAt = Instant.now();
    }
}
