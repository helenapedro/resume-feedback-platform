package com.pedro.resumeapi.dto;

import com.pedro.resumeapi.domain.ShareLink;

import java.time.Instant;
import java.util.UUID;

public record ShareLinkPublicDTO(
        UUID resumeId,
        UUID currentVersionId,
        ShareLink.Permission permission,
        Instant expiresAt
) {}
