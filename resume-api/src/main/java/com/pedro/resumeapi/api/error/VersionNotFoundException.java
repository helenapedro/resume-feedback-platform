package com.pedro.resumeapi.api.error;

import org.springframework.http.HttpStatus;

public class VersionNotFoundException extends DomainException {
    public VersionNotFoundException() {
        super(ErrorCode.VERSION_NOT_FOUND, HttpStatus.NOT_FOUND, "Resume version not found");
    }
}
