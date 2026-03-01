package com.pedro.resumeapi.comment.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCommentRequest(
        @NotBlank String body,
        String anchorRef
) {}
