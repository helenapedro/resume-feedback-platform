package com.pedro.resumeapi.ai.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AiFeedbackDTO(
        UUID resumeId,
        UUID resumeVersionId,
        UUID jobId,
        int feedbackVersion,
        String mongoDocId,
        String model,
        String promptVersion,
        Instant createdAt,
        String summary,
        List<String> strengths,
        List<String> improvements
) {}
