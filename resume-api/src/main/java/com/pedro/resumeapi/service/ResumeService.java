package com.pedro.resumeapi.service;

import com.pedro.resumeapi.domain.AiJob;
import com.pedro.resumeapi.domain.Resume;
import com.pedro.resumeapi.domain.ResumeVersion;
import com.pedro.resumeapi.domain.User;
import com.pedro.resumeapi.repository.AiJobRepository;
import com.pedro.resumeapi.repository.ResumeRepository;
import com.pedro.resumeapi.repository.ResumeVersionRepository;
import com.pedro.resumeapi.repository.UserRepository;
import com.pedro.resumeapi.storage.LocalStorageService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ResumeService {

    public static final UUID FIXED_OWNER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository versionRepository;
    private final AiJobRepository aiJobRepository;
    private final LocalStorageService storage;
    private final UserRepository userRepository;

    public List<Resume> listMyResumes() {
        return resumeRepository.findByOwner_IdOrderByCreatedAtDesc(FIXED_OWNER_ID);
    }

    public Resume getMyResume(UUID resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found"));
        if (!FIXED_OWNER_ID.equals(resume.getOwner().getId())) throw new IllegalArgumentException("Forbidden");
        return resume;
    }

    @Transactional
    public Resume createResume(String title, MultipartFile file) throws IOException {
        User owner = userRepository.findById(FIXED_OWNER_ID)
                .orElseThrow(() -> new IllegalStateException("Fixed owner not found in DB"));

        Resume resume = new Resume();
        resume.setOwner(owner);
        resume.setTitle(title == null || title.isBlank() ? "My Resume" : title);
        resumeRepository.save(resume);

        ResumeVersion v1 = buildVersion(resume, 1, file);
        versionRepository.save(v1);

        resume.setCurrentVersion(v1);
        resumeRepository.save(resume);

        AiJob job = new AiJob();
        job.setResumeVersion(v1);
        job.setStatus(AiJob.Status.PENDING);
        job.setAttemptCount(0);
        aiJobRepository.save(job);

        return resume;
    }

    @Transactional
    public ResumeVersion addVersion(UUID resumeId, MultipartFile file) throws IOException {
        Resume resume = getMyResume(resumeId);

        int next = versionRepository.findTopByResume_IdOrderByVersionNumberDesc(resumeId)
                .map(resumeVersion -> resumeVersion.getVersionNumber() + 1)
                .orElse(1);

        ResumeVersion resumeVersion = buildVersion(resume, next, file);
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

    private ResumeVersion buildVersion(Resume resume, int version, MultipartFile file) throws IOException {
        String original = (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank())
                ? "resume.pdf"
                : file.getOriginalFilename();

        String contentType = (file.getContentType() == null || file.getContentType().isBlank())
                ? "application/pdf"
                : file.getContentType();

        ResumeVersion resumeVersion = new ResumeVersion();
        resumeVersion.setResume(resume);
        resumeVersion.setVersionNumber(version);

        resumeVersion.setOriginalFilename(original);
        resumeVersion.setFileName(original);

        resumeVersion.setContentType(contentType);

        String storageKey = storage.store(FIXED_OWNER_ID, resume.getId(), version, file);
        resumeVersion.setStorageKey(storageKey);

        return resumeVersion;
    }

    public List<ResumeVersion> listVersions(UUID resumeId) {
        Resume resume = getMyResume(resumeId);
        return versionRepository.findByResume_IdOrderByVersionNumberDesc(resume.getId());
    }
}
