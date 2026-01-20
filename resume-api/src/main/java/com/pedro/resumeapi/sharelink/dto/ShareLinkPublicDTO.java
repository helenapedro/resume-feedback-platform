package com.pedro.resumeapi.sharelink.dto;

import com.pedro.resumeapi.sharelink.domain.ShareLink;

import java.time.Instant;
import java.util.UUID;

public record ShareLinkPublicDTO(
        UUID resumeId,
        UUID currentVersionId,
        ShareLink.Permission permission,
        Instant expiresAt
) {}
