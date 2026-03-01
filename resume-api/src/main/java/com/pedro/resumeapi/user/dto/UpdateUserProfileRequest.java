package com.pedro.resumeapi.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(max = 120) String fullName,
        @Size(max = 40) String phone,
        @Size(max = 1000) String bio,
        @Size(max = 500) String avatarUrl
) {}
