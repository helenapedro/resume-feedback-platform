package com.pedro.resumeworker.ai.service;

public class AiJobDomainException extends RuntimeException {

    private final String errorCode;

    public AiJobDomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
