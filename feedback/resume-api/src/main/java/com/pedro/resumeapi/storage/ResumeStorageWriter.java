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
    private final LocalStorageService localStorageService;
    private final ObjectProvider<S3StorageService> s3StorageService;

    public StorageResult store(UUID ownerId,
                               UUID resumeId,
                               int versionNumber,
                               MultipartFile file,
                               String originalFilename,
                               String contentType) throws IOException {
        if (storageProperties.getBackend() == StorageBackend.S3) {
            S3StorageService storage = s3StorageService.getIfAvailable();
            if (storage == null) {
                throw new IllegalStateException("S3 storage backend configured but service is unavailable");
            }
            return storage.store(ownerId, resumeId, versionNumber, file, originalFilename, contentType);
        }

        String storageKey = localStorageService.store(ownerId, resumeId, versionNumber, file);
        return new StorageResult(storageKey, null, null, null);
    }
}
