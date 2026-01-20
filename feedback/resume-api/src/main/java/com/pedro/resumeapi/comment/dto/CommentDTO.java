package com.pedro.resumeapi.comment.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentDTO(
        UUID id,
        UUID resumeVersionId,
        UUID authorUserId,     // null for guest
        String authorLabel,
        String body,
        String anchorRef,
        UUID parentCommentId,  // null if top-level
        Instant createdAt,
        Instant updatedAt
) {}
