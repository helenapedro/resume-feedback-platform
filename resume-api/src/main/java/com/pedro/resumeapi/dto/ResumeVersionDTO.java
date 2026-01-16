package com.pedro.resumeapi.dto;

import java.time.Instant;
import java.util.UUID;

public record ResumeVersionDTO(
        UUID id,
        int versionNumber,
        String originalFilename,
        String contentType,
        Long fileSizeBytes,
        UUID createdById,
        Instant createdAt
) {}
