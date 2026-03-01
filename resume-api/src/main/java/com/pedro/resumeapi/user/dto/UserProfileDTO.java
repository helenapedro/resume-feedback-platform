package com.pedro.resumeapi.user.dto;

import java.time.Instant;
import java.util.UUID;

public record UserProfileDTO(
        UUID id,
        String email,
        String role,
        boolean enabled,
        Instant createdAt,
        Instant lastLoginAt,
        String fullName,
        String phone,
        String bio,
        String avatarUrl
) {}
