package com.pedro.resumeapi.api.error;

public class InvalidShareLinkException extends RuntimeException {
    public InvalidShareLinkException() {
        super("Share link is invalid");
    }
}
