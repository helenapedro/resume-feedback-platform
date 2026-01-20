package com.pedro.resumeapi.comment.web;

import com.pedro.resumeapi.comment.dto.CommentDTO;
import com.pedro.resumeapi.comment.dto.CreateCommentRequest;
import com.pedro.resumeapi.comment.mapper.CommentMapper;
import com.pedro.resumeapi.comment.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/share/{token}/comments")
public class CommentPublicController {

    private final CommentService commentService;

    @GetMapping
    public List<CommentDTO> list(
            @PathVariable String token,
            HttpServletRequest request
    ) {
        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");

        return commentService.listPublic(token, ip, ua)
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

        var saved = commentService.createPublic(token, ip, ua, req);
        return CommentMapper.toDTO(saved);
    }
}
