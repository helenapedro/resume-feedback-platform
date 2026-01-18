package com.pedro.resumeapi.api.error;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        String code,
        String message,
        int status,
        String path,
        Instant timestamp,
        String traceId,
        Map<String, Object> details
) {
    public static ApiErrorResponse of(
            String code,
            String message,
            int status,
            String path,
            String traceId,
            Map<String, Object> details
    ) {
        return new ApiErrorResponse(code, message, status, path, Instant.now(), traceId, details);
    }
}
