package com.pedro.resumeapi.ai.service;

import com.pedro.resumeapi.ai.domain.AiFeedbackRef;
import com.pedro.resumeapi.ai.dto.AiFeedbackDTO;
import com.pedro.resumeapi.ai.mongo.AiFeedbackDocument;
import com.pedro.resumeapi.ai.mongo.AiFeedbackMongoRepository;
import com.pedro.resumeapi.ai.repository.AiFeedbackRefRepository;
import com.pedro.resumeapi.api.error.ForbiddenException;
import com.pedro.resumeapi.api.error.ResumeNotFoundException;
import com.pedro.resumeapi.api.error.VersionNotFoundException;
import com.pedro.resumeapi.resume.domain.Resume;
import com.pedro.resumeapi.resume.domain.ResumeVersion;
import com.pedro.resumeapi.resume.repository.ResumeRepository;
import com.pedro.resumeapi.resume.repository.ResumeVersionRepository;
import com.pedro.resumeapi.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiFeedbackService {

    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final AiFeedbackRefRepository aiFeedbackRefRepository;
    private final AiFeedbackMongoRepository aiFeedbackMongoRepository;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public AiFeedbackDTO getLatestFeedback(UUID resumeId, UUID versionId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(ResumeNotFoundException::new);
        if (!currentUser.id().equals(resume.getOwner().getId())) {
            throw new ForbiddenException("You do not own this resume");
        }

        ResumeVersion version = resumeVersionRepository.findByIdAndResume_Id(versionId, resumeId)
                .orElseThrow(VersionNotFoundException::new);

        AiFeedbackRef ref = aiFeedbackRefRepository
                .findByResumeVersion_IdOrderByFeedbackVersionDesc(version.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("AI_FEEDBACK_NOT_FOUND"));

        AiFeedbackDocument doc = aiFeedbackMongoRepository.findById(ref.getMongoDocId())
                .orElseThrow(() -> new IllegalArgumentException("AI_FEEDBACK_DOC_NOT_FOUND"));

        return new AiFeedbackDTO(
                resume.getId(),
                version.getId(),
                doc.getJobId(),
                ref.getFeedbackVersion(),
                ref.getMongoDocId(),
                ref.getModel(),
                ref.getPromptVersion(),
                doc.getCreatedAt(),
                doc.getSummary(),
                doc.getStrengths(),
                doc.getImprovements()
        );
    }
}
