package com.pedro.resumeapi.api.error;

import org.springframework.http.HttpStatus;

public class AiProgressNotFoundException extends DomainException {
    public AiProgressNotFoundException() {
        super(ErrorCode.AI_PROGRESS_NOT_FOUND, HttpStatus.NOT_FOUND, "AI progress not found");
    }
}
