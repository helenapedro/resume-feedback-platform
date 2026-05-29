package com.pedro.resumeapi.resume.service;

import com.pedro.resumeapi.api.error.UnsupportedResumeFileException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResumeUploadValidatorTest {

    private final ResumeUploadValidator validator = new ResumeUploadValidator();

    @Test
    void acceptsValidPdfUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "%PDF-1.7\ncontent".getBytes()
        );

        validator.validate(file);
    }

    @Test
    void rejectsNonPdfExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.jpg",
                "image/jpeg",
                "%PDF-1.7\ncontent".getBytes()
        );

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(UnsupportedResumeFileException.class);
    }

    @Test
    void rejectsFileWithoutPdfHeader() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "not a pdf".getBytes()
        );

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(UnsupportedResumeFileException.class);
    }
}
