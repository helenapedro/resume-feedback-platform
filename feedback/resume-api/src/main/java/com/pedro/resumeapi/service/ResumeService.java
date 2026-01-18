package com.pedro.resumeapi.service;

import com.pedro.resumeapi.api.error.ForbiddenException;
import com.pedro.resumeapi.api.error.ResumeNotFoundException;
import com.pedro.resumeapi.api.error.VersionNotFoundException;
import com.pedro.resumeapi.domain.AiJob;
import com.pedro.resumeapi.domain.Resume;
import com.pedro.resumeapi.domain.ResumeVersion;
import com.pedro.resumeapi.domain.User;
import com.pedro.resumeapi.dto.ResumeSummaryDTO;
import com.pedro.resumeapi.dto.ResumeVersionDTO;
import com.pedro.resumeapi.repository.AiJobRepository;
import com.pedro.resumeapi.repository.ResumeRepository;
import com.pedro.resumeapi.repository.ResumeVersionRepository;
import com.pedro.resumeapi.repository.UserRepository;
import com.pedro.resumeapi.security.CurrentUser;
import com.pedro.resumeapi.storage.LocalStorageService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ResumeService {

    private final CurrentUser currentUser;

    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository versionRepository;
    private final AiJobRepository aiJobRepository;
    private final LocalStorageService storage;
    private final UserRepository userRepository;
    private final ResumeVersionRepository resumeVersionRepository;

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

        var resumeVersion = buildVersion(resume, 1, file, owner);
        versionRepository.save(resumeVersion);

        resume.setCurrentVersion(resumeVersion);
        resumeRepository.save(resume);

        AiJob job = new AiJob();
        job.setResumeVersion(resumeVersion);
        job.setStatus(AiJob.Status.PENDING);
        job.setAttemptCount(0);
        aiJobRepository.save(job);

        return resume;
    }

    @Transactional
    public ResumeVersion addVersion(UUID resumeId, MultipartFile file) throws IOException {
        User owner = getUser();

        var resume = getMyResume(resumeId);

        int next = versionRepository.findTopByResume_IdOrderByVersionNumberDesc(resumeId)
                .map(resumeVersion -> resumeVersion.getVersionNumber() + 1)
                .orElse(1);

        ResumeVersion resumeVersion = buildVersion(resume, next, file, owner);
        versionRepository.save(resumeVersion);

        resume.setCurrentVersion(resumeVersion);
        resumeRepository.save(resume);

        AiJob job = new AiJob();
        job.setResumeVersion(resumeVersion);
        job.setStatus(AiJob.Status.PENDING);
        job.setAttemptCount(0);
        aiJobRepository.save(job);

        return resumeVersion;
    }

    private ResumeVersion buildVersion(Resume resume, int version, MultipartFile file, User owner) throws IOException {
        String original = (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank())
                ? "resume.pdf"
                : file.getOriginalFilename();

        String contentType = (file.getContentType() == null || file.getContentType().isBlank())
                ? "application/pdf"
                : file.getContentType();

        var resumeVersion = new ResumeVersion();
        resumeVersion.setResume(resume);
        resumeVersion.setVersionNumber(version);
        resumeVersion.setOriginalFilename(original);
        resumeVersion.setFileName(original);
        resumeVersion.setContentType(contentType);

        resumeVersion.setFileSizeBytes(file.getSize());
        resumeVersion.setCreatedBy(owner);

        String storageKey = storage.store(owner.getId(), resume.getId(), version, file);
        resumeVersion.setStorageKey(storageKey);

        return resumeVersion;
    }

    public List<ResumeVersion> listVersions(UUID resumeId) {
        Resume resume = getMyResume(resumeId);
        return versionRepository.findByResume_IdOrderByVersionNumberDesc(resume.getId());
    }

    // mappers
    public ResumeSummaryDTO toSummaryDTO(Resume r) {
        return new ResumeSummaryDTO(
                r.getId(),
                r.getTitle(),
                r.getCurrentVersion() != null ? r.getCurrentVersion().getId() : null,
                r.getCreatedAt()
        );
    }

    public ResumeVersionDTO toVersionDTO(ResumeVersion v) {
        return new ResumeVersionDTO(
                v.getId(),
                v.getVersionNumber(),
                v.getOriginalFilename(),
                v.getContentType(),
                v.getFileSizeBytes(),
                v.getCreatedBy() != null ? v.getCreatedBy().getId() : null,
                v.getCreatedAt()
        );
    }

    public record DownloadPayload(Resource resource, String filename, String contentType) {}

    @Transactional(readOnly = true)
    public DownloadPayload downloadVersion(UUID resumeId, UUID versionId) {
        getMyResume(resumeId);

        var resumeVersion = versionRepository.findByIdAndResume_Id(versionId, resumeId)
                .orElseThrow(VersionNotFoundException::new);

        var resource = storage.loadAsResource(resumeVersion.getStorageKey());

        String filename = (resumeVersion.getOriginalFilename() == null ||
                resumeVersion.getOriginalFilename().isBlank())
                ? "resume.pdf"
                : resumeVersion.getOriginalFilename();

        String contentType = (resumeVersion.getContentType() == null ||
                resumeVersion.getContentType().isBlank())
                ? "application/octet-stream"
                : resumeVersion.getContentType();

        return new DownloadPayload(resource, filename, contentType);
    }

    private User getUser() {
        User owner = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new IllegalStateException("Owner not found"));
        return owner;
    }
}
