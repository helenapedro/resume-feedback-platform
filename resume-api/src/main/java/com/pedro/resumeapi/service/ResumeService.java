package com.pedro.resumeapi.service;

import com.pedro.resumeapi.domain.AiJob;
import com.pedro.resumeapi.domain.Resume;
import com.pedro.resumeapi.domain.ResumeVersion;
import com.pedro.resumeapi.repository.AiJobRepository;
import com.pedro.resumeapi.repository.ResumeRepository;
import com.pedro.resumeapi.repository.ResumeVersionRepository;
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

    // fixed owner for now (JWT later)
    public static final UUID FIXED_OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository versionRepository;
    private final AiJobRepository aiJobRepository;
    private final LocalStorageService storage;

    public List<Resume> listMyResumes() {
        return resumeRepository.findByOwnerIdOrderByCreatedAtDesc(FIXED_OWNER_ID);
    }

    public Resume getMyResume(UUID resumeId) {
        Resume r = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found"));
        if (!FIXED_OWNER_ID.equals(r.getOwnerId())) throw new IllegalArgumentException("Forbidden");
        return r;
    }

    public List<ResumeVersion> listVersions(UUID resumeId) {
        Resume resume = getMyResume(resumeId);
        return versionRepository.findByResumeIdOrderByVersionNumberDesc(resume.getId());
    }

    @Transactional
    public Resume createResume(String title, MultipartFile file) throws IOException {
        Resume resume = new Resume();
        resume.setOwnerId(FIXED_OWNER_ID);
        resume.setTitle(title == null || title.isBlank() ? "My Resume" : title);
        resumeRepository.save(resume);

        // v1
        ResumeVersion v1 = new ResumeVersion();
        v1.setResumeId(resume.getId());
        v1.setVersionNumber(1);
        v1.setFileName(file.getOriginalFilename() == null ? "resume.pdf" : file.getOriginalFilename());
        v1.setContentType(file.getContentType() == null ? "application/pdf" : file.getContentType());

        String storageKey = storage.store(FIXED_OWNER_ID, resume.getId(), 1, file);
        v1.setStorageKey(storageKey);
        versionRepository.save(v1);

        resume.setCurrentVersionId(v1.getId());
        resumeRepository.save(resume);

        // AI job (PENDING) - for now it only creates; later we publish in the queue
        AiJob job = new AiJob();
        job.setResumeVersionId(v1.getId());
        job.setStatus(AiJob.Status.PENDING);
        job.setAttemptCount(0);
        aiJobRepository.save(job);

        return resume;
    }

    @Transactional
    public ResumeVersion addVersion(UUID resumeId, MultipartFile file) throws IOException {
        Resume resume = getMyResume(resumeId);

        int next = versionRepository.findTopByResumeIdOrderByVersionNumberDesc(resumeId)
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        ResumeVersion v = new ResumeVersion();
        v.setResumeId(resumeId);
        v.setVersionNumber(next);
        v.setFileName(file.getOriginalFilename() == null ? "resume.pdf" : file.getOriginalFilename());
        v.setContentType(file.getContentType() == null ? "application/pdf" : file.getContentType());

        String storageKey = storage.store(FIXED_OWNER_ID, resumeId, next, file);
        v.setStorageKey(storageKey);
        versionRepository.save(v);

        resume.setCurrentVersionId(v.getId());
        resumeRepository.save(resume);

        AiJob job = new AiJob();
        job.setResumeVersionId(v.getId());
        job.setStatus(AiJob.Status.PENDING);
        job.setAttemptCount(0);
        aiJobRepository.save(job);

        return v;
    }
}
