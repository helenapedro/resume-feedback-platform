package com.pedro.resumeapi.api.error;

import org.springframework.http.HttpStatus;

public class ResumeNotFoundException extends DomainException {
    public ResumeNotFoundException() {
        super(ErrorCode.RESUME_NOT_FOUND, HttpStatus.NOT_FOUND, "Resume not found");
    }
}
