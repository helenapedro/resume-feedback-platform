package com.pedro.resumeapi.sharelink.dto;

import com.pedro.resumeapi.sharelink.domain.ShareLink;

import java.time.Instant;
import java.util.UUID;

public record ShareLinkDTO (
    UUID id,
    ShareLink.Permission permission,
    Instant expiresAt,
    Instant revokedAt,
    Integer maxUses,
    int useCount,
    Instant createdAt,
    UUID createdBy

//    Resume resume;
){}
