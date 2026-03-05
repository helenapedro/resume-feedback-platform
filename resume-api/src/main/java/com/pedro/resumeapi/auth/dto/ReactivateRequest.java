package com.pedro.resumeapi.auth.dto;

public record ReactivateRequest(
        String email,
        String password
) {}

