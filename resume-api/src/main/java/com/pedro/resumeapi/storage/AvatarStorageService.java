package com.pedro.resumeapi.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvatarStorageService {

    private static final long MAX_AVATAR_BYTES = 2L * 1024L * 1024L; // 2 MB

    private final StorageProperties storageProperties;
    private final S3StorageProperties s3Properties;
    private final ObjectProvider<S3StorageService> s3StorageService;

    public String store(UUID userId, MultipartFile file) throws IOException {
        validate(file);

        String extension = resolveExtension(file);
        String key = "avatars/" + buildDatePrefix() + "_" + UUID.randomUUID() + "." + extension;
        String contentType = file.getContentType();

        if (storageProperties.getBackend() != StorageBackend.S3) {
            throw new IllegalStateException("Avatar upload requires S3 backend");
        }
        S3StorageService storage = s3StorageService.getIfAvailable();
        if (storage == null) {
            throw new IllegalStateException("S3 storage backend is not available");
        }
        storage.storeAvatar(key, file, contentType);
        return publicUrl(key);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Avatar file is required");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new IllegalArgumentException("Avatar exceeds 2MB limit");
        }
        String contentType = file.getContentType();
        if (!isSupportedContentType(contentType)) {
            throw new IllegalArgumentException("Unsupported avatar type. Use PNG, JPG or WEBP");
        }
    }

    private boolean isSupportedContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return false;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        return normalized.equals("image/png")
                || normalized.equals("image/jpeg")
                || normalized.equals("image/jpg")
                || normalized.equals("image/webp");
    }

    private String resolveExtension(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            return "png";
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/webp" -> "webp";
            default -> "png";
        };
    }

    private String publicUrl(String key) {
        String cloudfront = s3Properties.getCloudfrontUrl();
        if (StringUtils.hasText(cloudfront)) {
            return cloudfront.replaceAll("/+$", "") + "/" + key;
        }

        String bucket = s3Properties.getBucket();
        String region = s3Properties.getRegion();
        if (!StringUtils.hasText(bucket)) {
            throw new IllegalStateException("S3 bucket is not configured");
        }
        if (!StringUtils.hasText(region)) {
            return "https://" + bucket + ".s3.amazonaws.com/" + key;
        }
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    private String buildDatePrefix() {
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        return now.toString();
    }
}
