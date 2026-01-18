package com.pedro.resumeapi.api.error;

public enum ErrorCode {
    // Auth
    UNAUTHENTICATED,
    FORBIDDEN,

    // Resource
    RESUME_NOT_FOUND,
    VERSION_NOT_FOUND,
    VERSION_NOT_IN_RESUME,

    // Validation / Input
    VALIDATION_ERROR,
    INVALID_REQUEST,
    FILE_REQUIRED,
    FILE_TOO_LARGE,
    UNSUPPORTED_MEDIA_TYPE,

    // Storage / IO
    FILE_NOT_FOUND,
    STORAGE_ERROR,

    // Data / Integrity
    CONFLICT,
    DATA_INTEGRITY_VIOLATION,

    // Generic
    INTERNAL_ERROR
}
