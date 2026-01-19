package com.pedro.resumeapi.dto;

import com.pedro.resumeapi.domain.ShareLink;

import java.time.Instant;

public record CreateShareLinkRequest(
        ShareLink.Permission permission,
        Instant expiresAt,
        Integer maxUses
) { }
