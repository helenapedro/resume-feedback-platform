package com.pedro.resumeapi.api.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;
import org.springframework.http.ProblemDetail;

public class UnauthorizedException extends ErrorResponseException {

    public UnauthorizedException() {
        this("Invalid credentials");
    }

    public UnauthorizedException(String message) {
        super(
                HttpStatus.UNAUTHORIZED,
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.UNAUTHORIZED,
                        message
                ),
                null
        );
    }
}
