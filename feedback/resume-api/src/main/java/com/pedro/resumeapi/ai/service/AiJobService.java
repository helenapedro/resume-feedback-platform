package com.pedro.resumeapi.ai.service;

import com.pedro.resumeapi.ai.domain.AiJob;
import com.pedro.resumeapi.ai.kafka.AiJobEventPublisher;
import com.pedro.resumeapi.ai.mapper.AiJobMapper;
import com.pedro.resumeapi.ai.repository.AiJobRepository;
import com.pedro.resumeapi.api.error.AiJobNotFoundException;
import com.pedro.resumeapi.api.error.ForbiddenException;
import com.pedro.resumeapi.api.error.ResumeNotFoundException;
import com.pedro.resumeapi.api.error.VersionNotFoundException;
import com.pedro.resumeapi.resume.domain.Resume;
import com.pedro.resumeapi.resume.domain.ResumeVersion;
import com.pedro.resumeapi.resume.repository.ResumeRepository;
import com.pedro.resumeapi.resume.repository.ResumeVersionRepository;
import com.pedro.resumeapi.security.CurrentUser;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
public class AiJobService {
    private final AiJobRepository repo;
    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final CurrentUser currentUser;
    private final AiJobEventPublisher eventPublisher;

    @Transactional
    public AiJob createForVersion(ResumeVersion version) {
        return createForVersion(version, version.getId().toString());
    }

    @Transactional
    public AiJob createForVersion(ResumeVersion version, String idempotencyKey) {
        AiJob job = new AiJob();
        job.setResumeVersion(version);
        job.setStatus(AiJob.Status.PENDING);
        job.setAttemptCount(0);
        job.setIdempotencyKey(idempotencyKey);

        try {
            AiJob saved = repo.save(job);
            eventPublisher.publish(AiJobMapper.toMessage(saved));
            return saved;
        } catch (DataIntegrityViolationException ex) {
            return repo.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> ex);
        }
    }

    @Transactional
    public AiJob getLatestJobForVersion(UUID resumeId, UUID versionId) {
        ResumeVersion version = getOwnedVersion(resumeId, versionId);
        return repo.findTopByResumeVersion_IdOrderByCreatedAtDesc(version.getId())
                .orElseThrow(AiJobNotFoundException::new);
    }

    @Transactional
    public AiJob regenerateForVersion(UUID resumeId, UUID versionId) {
        ResumeVersion version = getOwnedVersion(resumeId, versionId);
        String idempotencyKey = version.getId() + ":regen:" + UUID.randomUUID();
        return createForVersion(version, idempotencyKey);
    }

    private ResumeVersion getOwnedVersion(UUID resumeId, UUID versionId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(ResumeNotFoundException::new);
        if (!currentUser.id().equals(resume.getOwner().getId())) {
            throw new ForbiddenException("You do not own this resume");
        }
        return resumeVersionRepository.findByIdAndResume_Id(versionId, resumeId)
                .orElseThrow(VersionNotFoundException::new);
    }
}
