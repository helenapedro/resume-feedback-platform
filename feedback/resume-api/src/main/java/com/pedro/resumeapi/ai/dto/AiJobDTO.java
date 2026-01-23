package com.pedro.resumeapi.ai.dto;

import java.time.Instant;
import java.util.UUID;

public record AiJobDTO(
        UUID id,
        UUID resumeVersionId,
        String status,
        int attemptCount,
        Instant createdAt,
        Instant updatedAt,
        Instant startedAt,
        Instant finishedAt,
        String errorCode,
        String errorDetail,
        Instant nextRetryAt
) {}
