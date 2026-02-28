package com.pedro.resumeapi.resume.service;

import com.pedro.resumeapi.api.error.ForbiddenException;
import com.pedro.resumeapi.api.error.ResumeNotFoundException;
import com.pedro.resumeapi.accessaudit.repository.AccessAuditRepository;
import com.pedro.resumeapi.ai.mongo.AiFeedbackMongoRepository;
import com.pedro.resumeapi.ai.repository.AiFeedbackRefRepository;
import com.pedro.resumeapi.resume.domain.Resume;
import com.pedro.resumeapi.resume.domain.ResumeVersion;
import com.pedro.resumeapi.user.domain.User;
import com.pedro.resumeapi.resume.factory.ResumeVersionFactory;
import com.pedro.resumeapi.ai.repository.AiJobRepository;
import com.pedro.resumeapi.comment.repository.CommentRepository;
import com.pedro.resumeapi.resume.repository.ResumeRepository;
import com.pedro.resumeapi.resume.repository.ResumeVersionRepository;
import com.pedro.resumeapi.storage.LocalStorageService;
import com.pedro.resumeapi.storage.S3StorageService;
import com.pedro.resumeapi.storage.StorageBackend;
import com.pedro.resumeapi.storage.StorageProperties;
import com.pedro.resumeapi.user.repository.UserRepository;
import com.pedro.resumeapi.security.CurrentUser;
import com.pedro.resumeapi.ai.service.AiJobService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class ResumeService {

    private final CurrentUser currentUser;
    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository versionRepository;
    private final AiJobRepository aiJobRepository;
    private final AiFeedbackRefRepository aiFeedbackRefRepository;
    private final AiFeedbackMongoRepository aiFeedbackMongoRepository;
    private final CommentRepository commentRepository;
    private final AccessAuditRepository accessAuditRepository;
    private final UserRepository userRepository;
    private final ResumeVersionFactory versionFactory;
    private final AiJobService aiJobService;
    private final StorageProperties storageProperties;
    private final LocalStorageService localStorageService;
    private final ObjectProvider<S3StorageService> s3StorageService;

    public List<Resume> listMyResumes() {
        return resumeRepository.findByOwner_IdOrderByCreatedAtDesc(currentUser.id());
    }

    public Resume getMyResume(UUID resumeId) {
        var resume = resumeRepository.findById(resumeId)
                .orElseThrow(ResumeNotFoundException::new);
        if (!currentUser.id().equals(resume.getOwner().getId()))
            throw new ForbiddenException("You do not own this resume");
        return resume;
    }

    @Transactional
    public Resume createResume(String title, MultipartFile file) throws IOException {
        User owner = getUser();

        Resume resume = new Resume();
        resume.setOwner(owner);
        resume.setTitle(title == null || title.isBlank() ? "My Resume" : title);
        resumeRepository.save(resume);

        var resumeVersion = versionFactory.create(resume, 1, file, owner);
        versionRepository.save(resumeVersion);

        resume.setCurrentVersion(resumeVersion);
        resumeRepository.save(resume);

        aiJobService.createForVersion(resumeVersion);

        return resume;
    }

    @Transactional
    public ResumeVersion addVersion(UUID resumeId, MultipartFile file) throws IOException {
        User owner = getUser();

        var resume = getMyResume(resumeId);

        int next = versionRepository.findTopByResume_IdOrderByVersionNumberDesc(resumeId)
                .map(resumeVersion -> resumeVersion.getVersionNumber() + 1)
                .orElse(1);

        ResumeVersion resumeVersion = versionFactory.create(resume, next, file, owner);
        versionRepository.save(resumeVersion);

        resume.setCurrentVersion(resumeVersion);
        resumeRepository.save(resume);

        aiJobService.createForVersion(resumeVersion);

        return resumeVersion;
    }

    public List<ResumeVersion> listVersions(UUID resumeId) {
        Resume resume = getMyResume(resumeId);
        return versionRepository.findByResume_IdOrderByVersionNumberDesc(resume.getId());
    }

    @Transactional
    public void deleteResume(UUID resumeId) {
        Resume resume = getMyResume(resumeId);
        List<ResumeVersion> versions = versionRepository.findByResume_IdOrderByVersionNumberDesc(resumeId);

        cleanupStoredFilesBestEffort(versions);

        // Remove dependent rows explicitly to satisfy FK constraints.
        accessAuditRepository.deleteByResume_Id(resumeId);

        var refs = aiFeedbackRefRepository.findByResumeVersion_Resume_Id(resumeId);
        if (!refs.isEmpty()) {
            aiFeedbackMongoRepository.deleteAllById(
                    refs.stream().map(ref -> ref.getMongoDocId()).filter(StringUtils::hasText).toList()
            );
        }
        aiFeedbackRefRepository.deleteByResumeVersion_Resume_Id(resumeId);
        aiJobRepository.deleteByResumeVersion_Resume_Id(resumeId);
        commentRepository.deleteByResumeVersion_Resume_Id(resumeId);

        // Keep relation consistent before deleting versions.
        resume.setCurrentVersion(null);
        resumeRepository.save(resume);

        versionRepository.deleteByResume_Id(resumeId);
        resumeRepository.delete(resume);
    }

    private User getUser() {
        return userRepository.findById(currentUser.id())
                .orElseThrow(() -> new IllegalStateException("Owner not found"));
    }

    private void cleanupStoredFilesBestEffort(List<ResumeVersion> versions) {
        for (ResumeVersion version : versions) {
            try {
                if (storageProperties.getBackend() == StorageBackend.S3) {
                    S3StorageService s3 = s3StorageService.getIfAvailable();
                    if (s3 != null) {
                        s3.deleteObject(version.getS3Bucket(), version.getS3ObjectKey(), version.getS3VersionId());
                    }
                } else {
                    localStorageService.deleteByStorageKey(version.getStorageKey());
                }
            } catch (Exception ex) {
                log.warn("Failed to delete stored file for version {}: {}", version.getId(), ex.getMessage());
            }
        }
    }
}
