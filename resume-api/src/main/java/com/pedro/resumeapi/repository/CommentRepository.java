package com.pedro.resumeapi.repository;

import com.pedro.resumeapi.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    Page<Comment> findByResumeVersion_IdOrderByCreatedAtAsc(UUID versionId, Pageable pageable);
}

