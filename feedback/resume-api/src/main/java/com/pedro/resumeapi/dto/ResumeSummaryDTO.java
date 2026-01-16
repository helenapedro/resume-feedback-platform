package com.pedro.resumeapi.dto;


import java.time.Instant;
import java.util.UUID;


public record ResumeSummaryDTO(
        UUID id,
        String title,
        UUID currentVersionId,
        Instant createdAt
) {}

