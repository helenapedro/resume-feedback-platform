package com.pedro.resumeapi.ai.mapper;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.resumeapi.ai.domain.AiJob;
import com.pedro.resumeapi.ai.dto.AiJobDTO;

import java.time.Instant;
import java.util.UUID;

public class AiJobMapper {

    public static AiJobDTO toDTO(AiJob job) {
        UUID versionId = job.getResumeVersion() == null ? null : job.getResumeVersion().getId();
        String status = job.getStatus() == null ? null : job.getStatus().name();

        return new AiJobDTO(
                job.getId(),
                versionId,
                status,
                job.getAttemptCount(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                job.getStartedAt(),
                job.getFinishedAt(),
                job.getErrorCode(),
                job.getErrorDetail(),
                job.getNextRetryAt()
        );
    }

    public static AiJobRequestedMessage toMessage(AiJob job) {
        UUID resumeId = job.getResumeVersion() == null || job.getResumeVersion().getResume() == null
                ? null
                : job.getResumeVersion().getResume().getId();
        UUID ownerId = job.getResumeVersion() == null
                || job.getResumeVersion().getResume() == null
                || job.getResumeVersion().getResume().getOwner() == null
                ? null
                : job.getResumeVersion().getResume().getOwner().getId();
        Instant createdAt = job.getCreatedAt();

        return new AiJobRequestedMessage(
                job.getId(),
                resumeId,
                job.getResumeVersion() == null ? null : job.getResumeVersion().getId(),
                ownerId,
                createdAt
        );
    }
}
