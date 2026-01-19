package com.pedro.resumeapi.service;

import com.pedro.resumeapi.api.error.ForbiddenException;
import com.pedro.resumeapi.api.error.VersionNotFoundException;
import com.pedro.resumeapi.domain.ResumeVersion;
import com.pedro.resumeapi.repository.ResumeRepository;
import com.pedro.resumeapi.repository.ResumeVersionRepository;
import com.pedro.resumeapi.security.CurrentUser;
import com.pedro.resumeapi.storage.LocalStorageService;
import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class ResumeStorageService {
    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final LocalStorageService storage;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public DownloadPayload downloadVersionOwner(UUID resumeId, UUID versionId) {
        var resume = resumeRepository.findById(resumeId)
                .orElseThrow(VersionNotFoundException::new);

        if (!resume.getOwner().getId().equals(currentUser.id()))
            throw new ForbiddenException("You do not own this resume");

        return getDownloadPayload(resumeId, versionId);
    }

    @Transactional(readOnly = true)
    public DownloadPayload downloadVersionPublic(UUID resumeId, UUID versionId) {
        return getDownloadPayload(resumeId, versionId);
    }

    private DownloadPayload getDownloadPayload(UUID resumeId, UUID versionId) {
        ResumeVersion version = resumeVersionRepository.findByIdAndResume_Id(versionId, resumeId)
                .orElseThrow(VersionNotFoundException::new);

        Resource resource = storage.loadAsResource(version.getStorageKey());

        String filename = (version.getOriginalFilename() == null ||
                version.getOriginalFilename().isBlank())
                ? "resume.pdf"
                : version.getOriginalFilename();

        String contentType = (version.getContentType() == null ||
                version.getContentType().isBlank())
                ? "application/octet-stream"
                : version.getContentType();

        return new DownloadPayload(resource, filename, contentType);
    }

    public record DownloadPayload(Resource resource, String filename, String contentType) {}
}
