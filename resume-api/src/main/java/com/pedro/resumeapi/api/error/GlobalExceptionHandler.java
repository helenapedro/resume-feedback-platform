package com.pedro.resumeapi.api.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // TODO: replace with real traceId from OTel/MDC
    private String traceId() {
        return UUID.randomUUID().toString();
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiErrorResponse> handleDomain(
            DomainException ex,
            HttpServletRequest req
    ) {
        var body = ApiErrorResponse.of(
                ex.getCode().name(),
                ex.getMessage(),
                ex.getStatus().value(),
                req.getRequestURI(),
                traceId(),
                ex.getDetails()
        );
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler({
            ShareLinkNotFoundException.class,
            InvalidShareLinkException.class
    })
    public ResponseEntity<ApiErrorResponse> handleShareLinkNotFoundOrInvalid(HttpServletRequest req) {
        // Deliberately 404 for security: do not reveal whether token exists.
        var body = ApiErrorResponse.of(
                ErrorCode.SHARE_LINK_NOT_FOUND.name(),
                "Share link not found",
                HttpStatus.NOT_FOUND.value(),
                req.getRequestURI(),
                traceId(),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler({
            ShareLinkExpiredException.class,
            ShareLinkRevokedException.class,
            ShareLinkMaxUsesReachedException.class
    })
    public ResponseEntity<ApiErrorResponse> handleShareLinkGone(RuntimeException ex, HttpServletRequest req) {
        // 410 GONE for expired/revoked/exhausted
        ErrorCode code = ErrorCode.SHARE_LINK_GONE;

        String message = "Share link is no longer valid";
        if (ex instanceof ShareLinkExpiredException) message = "Share link has expired";
        if (ex instanceof ShareLinkRevokedException) message = "Share link has been revoked";
        if (ex instanceof ShareLinkMaxUsesReachedException) message = "Share link usage limit reached";

        var body = ApiErrorResponse.of(
                code.name(),
                message,
                HttpStatus.GONE.value(),
                req.getRequestURI(),
                traceId(),
                null
        );
        return ResponseEntity.status(HttpStatus.GONE).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest req
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        details.put("fieldErrors", fieldErrors);

        var body = ApiErrorResponse.of(
                ErrorCode.VALIDATION_ERROR.name(),
                "Validation failed",
                HttpStatus.BAD_REQUEST.value(),
                req.getRequestURI(),
                traceId(),
                details
        );
        return ResponseEntity.badRequest().body(body);
    }


    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuth(
            AuthenticationException ex,
            HttpServletRequest req
    ) {
        var body = ApiErrorResponse.of(
                ErrorCode.UNAUTHENTICATED.name(),
                "Authentication required",
                HttpStatus.UNAUTHORIZED.value(),
                req.getRequestURI(),
                traceId(),
                null
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest req
    ) {
        var body = ApiErrorResponse.of(
                ErrorCode.FORBIDDEN.name(),
                "Forbidden",
                HttpStatus.FORBIDDEN.value(),
                req.getRequestURI(),
                traceId(),
                null
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }


    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest req
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("reason", "Database constraint violation");

        var body = ApiErrorResponse.of(
                ErrorCode.DATA_INTEGRITY_VIOLATION.name(),
                "Request violates data constraints",
                HttpStatus.CONFLICT.value(),
                req.getRequestURI(),
                traceId(),
                details
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }


    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ApiErrorResponse> handleSpringErrorResponse(
            ErrorResponseException ex,
            HttpServletRequest req
    ) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());

        String message = (ex.getBody() != null && ex.getBody().getDetail() != null)
                ? ex.getBody().getDetail()
                : "Invalid request";

        var body = ApiErrorResponse.of(
                ErrorCode.INVALID_REQUEST.name(),
                message,
                status.value(),
                req.getRequestURI(),
                traceId(),
                null
        );
        return ResponseEntity.status(status).body(body);
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            IllegalArgumentException ex,
            HttpServletRequest req
    ) {
        var body = ApiErrorResponse.of(
                ErrorCode.INVALID_REQUEST.name(),
                ex.getMessage() == null ? "Invalid request" : ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                req.getRequestURI(),
                traceId(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnknown(
            Exception ex,
            HttpServletRequest req
    ) {
        var body = ApiErrorResponse.of(
                ErrorCode.INTERNAL_ERROR.name(),
                "Unexpected error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                req.getRequestURI(),
                traceId(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
