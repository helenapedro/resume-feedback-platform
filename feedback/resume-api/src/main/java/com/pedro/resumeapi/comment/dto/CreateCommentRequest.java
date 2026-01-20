package com.pedro.resumeapi.comment.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateCommentRequest(
        @NotBlank String body,
        String anchorRef,
        UUID parentCommentId,
        String guestLabel
) {}