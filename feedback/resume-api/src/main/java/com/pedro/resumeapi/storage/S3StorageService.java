package com.pedro.resumeapi.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.backend", havingValue = "S3")
public class S3StorageService {

    private final S3Client s3Client;
    private final S3StorageProperties properties;

    public StorageResult store(UUID ownerId,
                               UUID resumeId,
                               int versionNumber,
                               MultipartFile file,
                               String originalFilename,
                               String contentType) throws IOException {
        if (!StringUtils.hasText(properties.getBucket())) {
            throw new IllegalStateException("S3 bucket is not configured");
        }
        String safeName = sanitizeFilename(originalFilename);
        String key = ownerId + "/" + resumeId + "/v" + versionNumber + "_" + safeName;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectResponse response = s3Client.putObject(
                request,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );

        return new StorageResult(key, properties.getBucket(), key, response.versionId());
    }

    private String sanitizeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "resume.pdf";
        }
        return filename
                .replace("\"", "")
                .replace("\r", "")
                .replace("\n", "")
                .replace("\\", "_")
                .replace("/", "_");
    }
}
