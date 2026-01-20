package com.pedro.resumeapi.comment.web;

import com.pedro.resumeapi.comment.dto.CommentDTO;
import com.pedro.resumeapi.comment.dto.CreateCommentRequest;
import com.pedro.resumeapi.comment.mapper.CommentMapper;
import com.pedro.resumeapi.comment.service.CommentService;
import com.pedro.resumeapi.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/resumes/{resumeId}/versions/{versionId}/comments")
public class CommentOwnerController {

    private final CommentService commentService;
    private final CurrentUser currentUser;

    @GetMapping
    public List<CommentDTO> list(
            @PathVariable UUID resumeId,
            @PathVariable UUID versionId
    ) {
        return commentService.listOwner(resumeId, versionId)
                .stream()
                .map(CommentMapper::toDTO)
                .toList();
    }

    @PostMapping
    public CommentDTO create(
            @PathVariable UUID resumeId,
            @PathVariable UUID versionId,
            @Valid @RequestBody CreateCommentRequest req
    ) {
        var saved = commentService.createOwner(resumeId, versionId, currentUser.id(), req);
        return CommentMapper.toDTO(saved);
    }
}
