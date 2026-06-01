package com.pedro.resumeapi.resume.service;

import com.pedro.resumeapi.api.error.ForbiddenException;
import com.pedro.resumeapi.api.error.VersionNotFoundException;
import com.pedro.resumeapi.resume.domain.ResumeVersion;
import com.pedro.resumeapi.resume.repository.ResumeRepository;
import com.pedro.resumeapi.resume.repository.ResumeVersionRepository;
import com.pedro.resumeapi.security.CurrentUser;
import com.pedro.resumeapi.storage.S3PresignService;
import com.pedro.resumeapi.storage.S3StorageService;
import com.pedro.resumeapi.storage.StorageBackend;
import com.pedro.resumeapi.storage.StorageProperties;
import com.pedro.resumeapi.storage.LocalStorageService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
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
    private final S3PresignService presignService;
    private final StorageProperties storageProperties;
    private final CurrentUser currentUser;
    private final ObjectProvider<LocalStorageService> localStorageService;
    private final ObjectProvider<S3StorageService> s3StorageService;

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
    public String previewVersionUrlOwner(UUID resumeId, UUID versionId, String localFallbackUrl) {
        DownloadPayload payload = previewVersionOwner(resumeId, versionId);

        if (payload.isPresigned()) {
            return payload.presignedUrl();
        }

        return localFallbackUrl;
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

        if (storageProperties.getBackend() == StorageBackend.LOCAL) {
            LocalStorageService local = localStorageService.getIfAvailable();
            if (local == null) {
                throw new IllegalStateException("Local storage backend is not available");
            }
            Resource resource = local.loadAsResource(version.getStorageKey());
            return new DownloadPayload(resource, filename, contentType, null);
        }

        if (!attachment) {
            S3StorageService s3 = s3StorageService.getIfAvailable();
            if (s3 == null) {
                throw new IllegalStateException("S3 storage backend is not available");
            }
            Resource resource = s3.loadAsResource(version.getS3Bucket(), version.getS3ObjectKey(), version.getS3VersionId());
            return new DownloadPayload(resource, filename, contentType, null);
        }

        var presigned = presignService.presignDownload(version, safeName, contentType)
                .map(Object::toString)
                .orElse(null);

        if (StringUtils.hasText(presigned)) {
            return new DownloadPayload(null, filename, contentType, presigned);
        }

        if (StringUtils.hasText(version.getS3Bucket()) || StringUtils.hasText(version.getS3ObjectKey())) {
            throw new IllegalStateException("S3 download requested but presigned URL is unavailable");
        }

        throw new IllegalStateException("Resume version is missing S3 object metadata");
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
