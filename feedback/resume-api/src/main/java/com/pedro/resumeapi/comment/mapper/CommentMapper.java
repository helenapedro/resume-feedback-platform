package com.pedro.resumeapi.comment.mapper;

import com.pedro.resumeapi.comment.domain.Comment;
import com.pedro.resumeapi.comment.dto.CommentDTO;

import java.util.UUID;

public class CommentMapper {

    public static CommentDTO toDTO(Comment c) {
        UUID authorId = c.getAuthorUser() == null ? null : c.getAuthorUser().getId();
        UUID parentId = c.getParentComment() == null ? null : c.getParentComment().getId();

        return new CommentDTO(
                c.getId(),
                c.getResumeVersion().getId(),
                authorId,
                c.getAuthorLabel(),
                c.getBody(),
                c.getAnchorRef(),
                parentId,
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
