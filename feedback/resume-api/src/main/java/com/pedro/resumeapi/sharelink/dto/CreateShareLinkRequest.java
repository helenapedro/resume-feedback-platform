package com.pedro.resumeapi.sharelink.dto;

import com.pedro.resumeapi.sharelink.domain.ShareLink;

import java.time.Instant;

public record CreateShareLinkRequest(
        ShareLink.Permission permission,
        Instant expiresAt,
        Integer maxUses
) { }
