package com.pedro.resumeapi.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "share_links",
        indexes = {
                @Index(name = "idx_share_links_resume", columnList = "resume_id"),
                @Index(name = "idx_share_links_expires", columnList = "expires_at")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_share_links_token_hash", columnNames = "token_hash"))
@Getter @Setter
public class ShareLink {

    public enum Permission { VIEW, COMMENT }

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash; // sha-256 hex

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 20)
    private Permission permission;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "use_count", nullable = false)
    private int useCount = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public boolean isRevoked() { return revokedAt != null; }
    public boolean isExpired(Instant now) { return expiresAt != null && expiresAt.isBefore(now); }
    public boolean isExhausted() { return maxUses != null && useCount >= maxUses; }
}
