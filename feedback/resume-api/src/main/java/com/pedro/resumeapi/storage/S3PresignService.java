package com.pedro.resumeapi.storage;

import com.pedro.resumeapi.resume.domain.ResumeVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class S3PresignService {

    private final ObjectProvider<S3Presigner> presignerProvider;
    private final S3StorageProperties properties;

    public Optional<URL> presignDownload(ResumeVersion version, String safeFilename, String contentType) {
        if (!StringUtils.hasText(version.getS3Bucket()) || !StringUtils.hasText(version.getS3ObjectKey())) {
            return Optional.empty();
        }

        S3Presigner presigner = presignerProvider.getIfAvailable();
        if (presigner == null) {
            return Optional.empty();
        }

        GetObjectRequest.Builder getObjectRequest = GetObjectRequest.builder()
                .bucket(version.getS3Bucket())
                .key(version.getS3ObjectKey())
                .responseContentDisposition("attachment; filename=\"" + safeFilename + "\"")
                .responseContentType(contentType);

        if (StringUtils.hasText(version.getS3VersionId())) {
            getObjectRequest.versionId(version.getS3VersionId());
        }

        Duration signatureDuration = properties.getPresignExpiration();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(signatureDuration)
                .getObjectRequest(getObjectRequest.build())
                .build();

        return Optional.of(presigner.presignGetObject(presignRequest).url());
    }
}
