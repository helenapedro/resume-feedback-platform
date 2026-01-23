package com.pedro.resumeapi.api.error;

import org.springframework.http.HttpStatus;

public class AiJobNotFoundException extends DomainException {
    public AiJobNotFoundException() {
        super(ErrorCode.AI_JOB_NOT_FOUND, HttpStatus.NOT_FOUND, "AI job not found");
    }
}
