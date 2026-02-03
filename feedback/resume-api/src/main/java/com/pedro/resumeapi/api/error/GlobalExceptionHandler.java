package com.pedro.resumeapi.api.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.io.IOException;
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


    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingPart(
            MissingServletRequestPartException ex,
            HttpServletRequest req
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("part", ex.getRequestPartName());

        var body = ApiErrorResponse.of(
                ErrorCode.FILE_REQUIRED.name(),
                "File is required",
                HttpStatus.BAD_REQUEST.value(),
                req.getRequestURI(),
                traceId(),
                details
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMedia(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest req
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("supported", ex.getSupportedMediaTypes());

        var body = ApiErrorResponse.of(
                ErrorCode.UNSUPPORTED_MEDIA_TYPE.name(),
                "Content-Type not supported. Use multipart/form-data",
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                req.getRequestURI(),
                traceId(),
                details
        );
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(body);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiErrorResponse> handleMultipart(
            MultipartException ex,
            HttpServletRequest req
    ) {
        var body = ApiErrorResponse.of(
                ErrorCode.INVALID_REQUEST.name(),
                ex.getMessage() == null ? "Invalid multipart request" : ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                req.getRequestURI(),
                traceId(),
                exceptionDetails(ex)
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException ex,
            HttpServletRequest req
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("maxBytes", ex.getMaxUploadSize());

        var body = ApiErrorResponse.of(
                ErrorCode.FILE_TOO_LARGE.name(),
                "Uploaded file exceeds the maximum allowed size",
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                req.getRequestURI(),
                traceId(),
                details
        );
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiErrorResponse> handleIOException(
            IOException ex,
            HttpServletRequest req
    ) {
        var body = ApiErrorResponse.of(
                ErrorCode.STORAGE_ERROR.name(),
                ex.getMessage() == null ? "Storage operation failed" : ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                req.getRequestURI(),
                traceId(),
                exceptionDetails(ex)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
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

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest req
    ) {
        String message = ex.getMessage() == null ? "Server is misconfigured" : ex.getMessage();
        var body = ApiErrorResponse.of(
                ErrorCode.INTERNAL_ERROR.name(),
                message,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                req.getRequestURI(),
                traceId(),
                exceptionDetails(ex)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnknown(
            Exception ex,
            HttpServletRequest req
    ) {
        String message = ex.getMessage() == null ? "Unexpected error" : ex.getMessage();
        var body = ApiErrorResponse.of(
                ErrorCode.INTERNAL_ERROR.name(),
                message,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                req.getRequestURI(),
                traceId(),
                exceptionDetails(ex)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private Map<String, Object> exceptionDetails(Exception ex) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("exception", ex.getClass().getSimpleName());
        if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
            details.put("message", ex.getMessage());
        }
        Throwable cause = ex.getCause();
        if (cause != null) {
            details.put("cause", cause.getClass().getSimpleName());
            if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
                details.put("causeMessage", cause.getMessage());
            }
        }
        return details.isEmpty() ? null : details;
    }
}
