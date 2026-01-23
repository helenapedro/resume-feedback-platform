package com.pedro.common.ai;

import java.time.Instant;
import java.util.UUID;

public record AiJobRequestedMessage(
        UUID jobId,
        UUID resumeId,
        UUID resumeVersionId,
        UUID ownerId,
        Instant createdAt
) {}
