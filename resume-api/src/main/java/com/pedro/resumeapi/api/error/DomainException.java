package com.pedro.resumeapi.api.error;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class DomainException extends RuntimeException {

    private final ErrorCode code;
    private final HttpStatus status;
    private final Map<String, Object> details;

    public DomainException(ErrorCode code, HttpStatus status, String message) {
        this(code, status, message, null);
    }

    public DomainException(ErrorCode code, HttpStatus status, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = details;
    }

    public ErrorCode getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
