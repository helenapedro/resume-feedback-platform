package com.pedro.resumeapi.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumeStorageWriter {

    private final StorageProperties storageProperties;
    private final ObjectProvider<S3StorageService> s3StorageService;
    private final ObjectProvider<LocalStorageService> localStorageService;

    public StorageResult store(UUID ownerId,
            UUID resumeId,
            int versionNumber,
            MultipartFile file,
            String originalFilename,
            String contentType) throws IOException {

        if (storageProperties.getBackend() == StorageBackend.LOCAL) {
            LocalStorageService local = localStorageService.getIfAvailable();
            if (local == null) {
                throw new IllegalStateException("Local storage backend is not available");
            }
            String storageKey = local.store(ownerId, resumeId, versionNumber, file);
            return new StorageResult(storageKey, null, null, null);
        }

        S3StorageService s3 = s3StorageService.getIfAvailable();
        if (s3 == null) {
            throw new IllegalStateException("S3 storage backend is not available");
        }
        return s3.store(ownerId, resumeId, versionNumber, file, originalFilename, contentType);
    }
}
