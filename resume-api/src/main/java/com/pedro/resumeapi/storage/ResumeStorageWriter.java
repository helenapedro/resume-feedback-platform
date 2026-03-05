package com.pedro.resumeapi.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumeStorageWriter {

    private final StorageProperties storageProperties;
    private final S3StorageService s3StorageService;

    public StorageResult store(UUID ownerId,
            UUID resumeId,
            int versionNumber,
            MultipartFile file,
            String originalFilename,
            String contentType) throws IOException {

        if (storageProperties.getBackend() != StorageBackend.S3) {
            throw new IllegalStateException("Only S3 storage backend is supported");
        }

        return s3StorageService.store(ownerId, resumeId, versionNumber, file, originalFilename, contentType);
    }
}
