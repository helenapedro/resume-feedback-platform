package com.pedro.resumeworker.ai.service;

import com.pedro.resumeworker.ai.domain.ResumeVersion;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Component
@Slf4j
public class ResumeTextExtractor {

    private final ObjectProvider<S3Client> s3ClientProvider;
    private final int maxResumeChars;

    public ResumeTextExtractor(ObjectProvider<S3Client> s3ClientProvider,
                               @Value("${app.ai-feedback.max-resume-chars:12000}") int maxResumeChars) {
        this.s3ClientProvider = s3ClientProvider;
        this.maxResumeChars = maxResumeChars;
    }

    public Optional<String> extract(ResumeVersion version) {
        if (version == null) {
            return Optional.empty();
        }

        try {
            byte[] bytes = null;

            if (StringUtils.hasText(version.getS3Bucket()) && StringUtils.hasText(version.getS3ObjectKey())) {
                S3Client s3 = s3ClientProvider.getIfAvailable();
                if (s3 != null) {
                    GetObjectRequest.Builder req = GetObjectRequest.builder()
                            .bucket(version.getS3Bucket())
                            .key(version.getS3ObjectKey());
                    if (StringUtils.hasText(version.getS3VersionId())) {
                        req.versionId(version.getS3VersionId());
                    }
                    ResponseBytes<GetObjectResponse> objectBytes = s3.getObjectAsBytes(req.build());
                    bytes = objectBytes.asByteArray();
                }
            }

            if (bytes == null && StringUtils.hasText(version.getStorageKey())) {
                Path localPath = Path.of(version.getStorageKey()).toAbsolutePath().normalize();
                if (Files.exists(localPath)) {
                    bytes = Files.readAllBytes(localPath);
                }
            }

            if (bytes == null || bytes.length == 0) {
                return Optional.empty();
            }

            String text = extractTextFromPdf(bytes);
            if (!StringUtils.hasText(text)) {
                return Optional.empty();
            }

            String normalized = normalizeText(text);
            if (normalized.length() > maxResumeChars) {
                normalized = normalized.substring(0, maxResumeChars);
            }
            return Optional.of(normalized);
        } catch (Exception ex) {
            log.warn("Could not extract resume text for resumeVersionId={}: {}",
                    version.getId(), ex.getMessage());
            return Optional.empty();
        }
    }

    private String extractTextFromPdf(byte[] pdfBytes) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private String normalizeText(String text) {
        return text
                .replace('\u0000', ' ')
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .replaceAll(" {2,}", " ")
                .trim();
    }
}

