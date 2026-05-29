package com.pedro.resumeapi.resume.service;

import com.pedro.resumeapi.api.error.FileRequiredException;
import com.pedro.resumeapi.api.error.UnsupportedResumeFileException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

@Service
public class ResumeUploadValidator {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            PDF_CONTENT_TYPE,
            "application/x-pdf"
    );
    private static final byte[] PDF_HEADER = {'%', 'P', 'D', 'F', '-'};

    public void validate(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new FileRequiredException();
        }

        validateFilename(file.getOriginalFilename());
        validateContentType(file.getContentType());
        validatePdfHeader(file);
    }

    public String normalizedContentType() {
        return PDF_CONTENT_TYPE;
    }

    private void validateFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return;
        }

        String lower = filename.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".pdf")) {
            throw new UnsupportedResumeFileException("Only PDF resume uploads are supported. Upload a .pdf file.");
        }
    }

    private void validateContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return;
        }

        String normalized = contentType.toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(normalized)) {
            throw new UnsupportedResumeFileException("Only PDF resume uploads are supported.");
        }
    }

    private void validatePdfHeader(MultipartFile file) throws IOException {
        byte[] header = new byte[PDF_HEADER.length];
        int read;
        try (InputStream input = file.getInputStream()) {
            read = input.read(header);
        }

        if (read < PDF_HEADER.length) {
            throw new UnsupportedResumeFileException("Uploaded file is not a valid PDF document.");
        }

        for (int i = 0; i < PDF_HEADER.length; i++) {
            if (header[i] != PDF_HEADER[i]) {
                throw new UnsupportedResumeFileException("Uploaded file is not a valid PDF document.");
            }
        }
    }
}
