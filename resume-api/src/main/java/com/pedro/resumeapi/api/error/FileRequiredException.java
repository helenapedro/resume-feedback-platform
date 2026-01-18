package com.pedro.resumeapi.api.error;

import org.springframework.http.HttpStatus;

public class FileRequiredException extends DomainException {
    public FileRequiredException() {
        super(ErrorCode.FILE_REQUIRED, HttpStatus.BAD_REQUEST, "File is required");
    }
}
