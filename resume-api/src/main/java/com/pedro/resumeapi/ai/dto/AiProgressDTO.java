package com.pedro.resumeapi.ai.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AiProgressDTO(
        UUID resumeId,
        UUID resumeVersionId,
        UUID baselineResumeVersionId,
        UUID jobId,
        int progressVersion,
        String mongoDocId,
        String model,
        String promptVersion,
        Instant createdAt,
        String summary,
        String progressStatus,
        Integer progressScore,
        List<String> improvedAreas,
        List<String> unchangedIssues,
        List<String> newIssues
) {}
