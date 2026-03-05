package com.pedro.resumeapi.comment.repository;

import com.pedro.resumeapi.comment.domain.Comment;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByResumeVersion_IdOrderByCreatedAtAsc(UUID resumeVersionId);

    Optional<Comment> findByIdAndResumeVersion_Id(UUID id, UUID resumeVersionId);

    List<Comment> findByParentComment_Id(UUID parentCommentId);

    void deleteByResumeVersion_Resume_Id(UUID resumeId);

    @Modifying
    @Query("""
            update Comment c
            set c.authorUser = null,
                c.authorLabel = :authorLabel
            where c.authorUser.id = :userId
            """)
    int anonymizeByAuthorUserId(@Param("userId") UUID userId, @Param("authorLabel") String authorLabel);
}
