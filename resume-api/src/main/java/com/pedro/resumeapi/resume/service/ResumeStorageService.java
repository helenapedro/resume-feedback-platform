package com.pedro.resumeapi.resume.service;

import com.pedro.resumeapi.api.error.ForbiddenException;
import com.pedro.resumeapi.api.error.VersionNotFoundException;
import com.pedro.resumeapi.resume.domain.ResumeVersion;
import com.pedro.resumeapi.resume.repository.ResumeRepository;
import com.pedro.resumeapi.resume.repository.ResumeVersionRepository;
import com.pedro.resumeapi.security.CurrentUser;
import com.pedro.resumeapi.storage.LocalStorageService;
import com.pedro.resumeapi.storage.S3PresignService;
import com.pedro.resumeapi.storage.StorageBackend;
import com.pedro.resumeapi.storage.StorageProperties;
import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@AllArgsConstructor
public class ResumeStorageService {
    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final LocalStorageService storage;
    private final S3PresignService presignService;
    private final StorageProperties storageProperties;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public DownloadPayload downloadVersionOwner(UUID resumeId, UUID versionId) {
        var resume = resumeRepository.findById(resumeId)
                .orElseThrow(VersionNotFoundException::new);

        if (!resume.getOwner().getId().equals(currentUser.id()))
            throw new ForbiddenException("You do not own this resume");

        return getPayload(resumeId, versionId, true);
    }

    @Transactional(readOnly = true)
    public DownloadPayload downloadVersionPublic(UUID resumeId, UUID versionId) {
        return getPayload(resumeId, versionId, true);
    }

    @Transactional(readOnly = true)
    public DownloadPayload previewVersionOwner(UUID resumeId, UUID versionId) {
        var resume = resumeRepository.findById(resumeId)
                .orElseThrow(VersionNotFoundException::new);

        if (!resume.getOwner().getId().equals(currentUser.id()))
            throw new ForbiddenException("You do not own this resume");

        return getPayload(resumeId, versionId, false);
    }

    @Transactional(readOnly = true)
    public DownloadPayload previewVersionPublic(UUID resumeId, UUID versionId) {
        return getPayload(resumeId, versionId, false);
    }

    private DownloadPayload getPayload(UUID resumeId, UUID versionId, boolean attachment) {
        ResumeVersion version = resumeVersionRepository.findByIdAndResume_Id(versionId, resumeId)
                .orElseThrow(VersionNotFoundException::new);

        String filename = (version.getOriginalFilename() == null ||
                version.getOriginalFilename().isBlank())
                ? "resume.pdf"
                : version.getOriginalFilename();

        String safeName = sanitizeFilename(filename);

        String contentType = (version.getContentType() == null ||
                version.getContentType().isBlank())
                ? "application/octet-stream"
                : version.getContentType();

        if (storageProperties.getBackend() == StorageBackend.S3) {
            var presigned = (attachment
                    ? presignService.presignDownload(version, safeName, contentType)
                    : presignService.presignPreview(version, safeName, contentType))
                    .map(Object::toString)
                    .orElse(null);

            if (StringUtils.hasText(presigned)) {
                return new DownloadPayload(null, filename, contentType, presigned);
            }

            if (StringUtils.hasText(version.getS3Bucket()) || StringUtils.hasText(version.getS3ObjectKey())) {
                throw new IllegalStateException("S3 download requested but presigned URL is unavailable");
            }
        }

        Resource resource = storage.loadAsResource(version.getStorageKey());

        return new DownloadPayload(resource, filename, contentType, null);
    }

    private String sanitizeFilename(String filename) {
        return filename
                .replace("\"", "")
                .replace("\r", "")
                .replace("\n", "")
                .replace("\\", "_")
                .replace("/", "_");
    }

    public record DownloadPayload(Resource resource, String filename, String contentType, String presignedUrl) {
        public boolean isPresigned() {
            return StringUtils.hasText(presignedUrl);
        }
    }
}
