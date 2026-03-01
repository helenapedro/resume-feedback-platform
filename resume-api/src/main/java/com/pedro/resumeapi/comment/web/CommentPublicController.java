package com.pedro.resumeapi.comment.web;

import com.pedro.resumeapi.comment.dto.CommentDTO;
import com.pedro.resumeapi.comment.dto.CreateCommentRequest;
import com.pedro.resumeapi.comment.dto.UpdateCommentRequest;
import com.pedro.resumeapi.comment.mapper.CommentMapper;
import com.pedro.resumeapi.comment.service.CommentService;
import com.pedro.resumeapi.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/share/{token}/comments")
public class CommentPublicController {

    private final CommentService commentService;
    private final CurrentUser currentUser;

    @GetMapping
    public List<CommentDTO> list(
            @PathVariable String token,
            HttpServletRequest request
    ) {
        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");

        return commentService.listPublic(token, ip, ua, currentUser.id())
                .stream()
                .map(CommentMapper::toDTO)
                .toList();
    }

    @PostMapping
    public CommentDTO create(
            @PathVariable String token,
            @Valid @RequestBody CreateCommentRequest req,
            HttpServletRequest request
    ) {
        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");

        var saved = commentService.createPublic(token, ip, ua, currentUser.id(), req);
        return CommentMapper.toDTO(saved);
    }

    @PatchMapping("/{commentId}")
    public CommentDTO update(
            @PathVariable String token,
            @PathVariable java.util.UUID commentId,
            @Valid @RequestBody UpdateCommentRequest req,
            HttpServletRequest request
    ) {
        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");

        var saved = commentService.updatePublic(token, commentId, currentUser.id(), ip, ua, req);
        return CommentMapper.toDTO(saved);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable String token,
            @PathVariable java.util.UUID commentId,
            HttpServletRequest request
    ) {
        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");

        commentService.deletePublic(token, commentId, currentUser.id(), ip, ua);
        return ResponseEntity.noContent().build();
    }
}
