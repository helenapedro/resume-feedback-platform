package com.pedro.resumeapi.api.error;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class UnsupportedResumeFileException extends DomainException {

    public UnsupportedResumeFileException(String message) {
        super(
                ErrorCode.UNSUPPORTED_MEDIA_TYPE,
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                message,
                Map.of("allowedFormats", "PDF")
        );
    }
}
