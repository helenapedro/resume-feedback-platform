package com.pedro.resumeapi.dto;

import com.pedro.resumeapi.domain.ShareLink;

import java.time.Instant;
import java.util.UUID;

public record CreateShareLinkResponse(
        UUID id,
        String token,
        ShareLink.Permission permission,
        Instant expiresAt,
        Integer maxUses
) {}
