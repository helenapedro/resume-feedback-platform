package com.pedro.resumeapi.ai.service;

import com.pedro.common.ai.mongo.AiProgressDocument;
import com.pedro.resumeapi.ai.domain.AiProgressRef;
import com.pedro.resumeapi.ai.dto.AiProgressDTO;
import com.pedro.resumeapi.ai.mongo.AiProgressMongoRepository;
import com.pedro.resumeapi.ai.repository.AiProgressRefRepository;
import com.pedro.resumeapi.api.error.AiProgressNotFoundException;
import com.pedro.resumeapi.api.error.ForbiddenException;
import com.pedro.resumeapi.api.error.ResumeNotFoundException;
import com.pedro.resumeapi.api.error.VersionNotFoundException;
import com.pedro.resumeapi.resume.domain.Resume;
import com.pedro.resumeapi.resume.domain.ResumeVersion;
import com.pedro.resumeapi.resume.repository.ResumeRepository;
import com.pedro.resumeapi.resume.repository.ResumeVersionRepository;
import com.pedro.resumeapi.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiProgressService {

    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final AiProgressRefRepository aiProgressRefRepository;
    private final AiProgressMongoRepository aiProgressMongoRepository;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public AiProgressDTO getLatestProgress(UUID resumeId, UUID versionId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(ResumeNotFoundException::new);
        if (!currentUser.id().equals(resume.getOwner().getId())) {
            throw new ForbiddenException("You do not own this resume");
        }

        ResumeVersion version = resumeVersionRepository.findByIdAndResume_Id(versionId, resumeId)
                .orElseThrow(VersionNotFoundException::new);

        AiProgressRef ref = aiProgressRefRepository
                .findTopByResumeVersion_IdOrderByProgressVersionDesc(version.getId())
                .orElseThrow(AiProgressNotFoundException::new);

        String mongoDocId = ref.getMongoDocId() == null ? null : ref.getMongoDocId().trim();
        AiProgressDocument doc = (mongoDocId == null || mongoDocId.isBlank())
                ? null
                : aiProgressMongoRepository.findById(mongoDocId).orElse(null);

        if (doc == null) {
            doc = aiProgressMongoRepository.findTopByResumeVersionIdOrderByCreatedAtDesc(version.getId())
                    .orElseThrow(AiProgressNotFoundException::new);
            log.warn("AI progress ref/doc mismatch for resumeVersionId={} (ref mongoDocId={}, fallback mongoDocId={})",
                    version.getId(), ref.getMongoDocId(), doc.getId());
        }

        return new AiProgressDTO(
                resume.getId(),
                version.getId(),
                doc.getBaselineResumeVersionId(),
                doc.getJobId(),
                ref.getProgressVersion(),
                ref.getMongoDocId(),
                ref.getModel(),
                ref.getPromptVersion(),
                doc.getCreatedAt(),
                doc.getSummary(),
                doc.getProgressStatus(),
                doc.getProgressScore(),
                doc.getImprovedAreas(),
                doc.getUnchangedIssues(),
                doc.getNewIssues()
        );
    }
}
