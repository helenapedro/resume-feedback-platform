package com.pedro.resumeapi.resume.dto;


import java.time.Instant;
import java.util.UUID;


public record ResumeSummaryDTO(
        UUID id,
        String title,
        UUID currentVersionId,
        Instant createdAt
) {}

