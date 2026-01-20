package com.pedro.resumeapi.resume.service;

import com.pedro.resumeapi.api.error.ForbiddenException;
import com.pedro.resumeapi.api.error.ResumeNotFoundException;
import com.pedro.resumeapi.resume.domain.Resume;
import com.pedro.resumeapi.resume.domain.ResumeVersion;
import com.pedro.resumeapi.user.domain.User;
import com.pedro.resumeapi.resume.factory.ResumeVersionFactory;
import com.pedro.resumeapi.ai.repository.AiJobRepository;
import com.pedro.resumeapi.resume.repository.ResumeRepository;
import com.pedro.resumeapi.resume.repository.ResumeVersionRepository;
import com.pedro.resumeapi.user.repository.UserRepository;
import com.pedro.resumeapi.security.CurrentUser;
import com.pedro.resumeapi.ai.service.AiJobService;
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

    private final CurrentUser currentUser;
    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository versionRepository;
    private final AiJobRepository aiJobRepository;
    private final UserRepository userRepository;
    private final ResumeVersionFactory versionFactory;
    private final AiJobService aiJobService;

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

    private User getUser() {
        return userRepository.findById(currentUser.id())
                .orElseThrow(() -> new IllegalStateException("Owner not found"));
    }
}