package com.pedro.resumeapi.comment.service;

import com.pedro.resumeapi.accessaudit.domain.AccessAudit;
import com.pedro.resumeapi.api.error.ForbiddenException;
import com.pedro.resumeapi.api.error.VersionNotFoundException;
import com.pedro.resumeapi.comment.domain.Comment;
import com.pedro.resumeapi.comment.dto.CreateCommentRequest;
import com.pedro.resumeapi.comment.repository.CommentRepository;
import com.pedro.resumeapi.resume.domain.Resume;
import com.pedro.resumeapi.resume.domain.ResumeVersion;
import com.pedro.resumeapi.resume.repository.ResumeVersionRepository;
import com.pedro.resumeapi.resume.service.ResumeService;
import com.pedro.resumeapi.sharelink.domain.ShareLink;
import com.pedro.resumeapi.sharelink.service.ShareLinkService;
import com.pedro.resumeapi.user.domain.User;
import com.pedro.resumeapi.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class CommentService {

    private final CommentRepository commentRepo;
    private final ResumeService resumeService;
    private final ResumeVersionRepository resumeVersionRepo;
    private final UserRepository userRepo;
    private final ShareLinkService shareLinkService;

    private final Clock clock = Clock.systemUTC();

    @Transactional
    public Comment createOwner(UUID resumeId, UUID versionId, UUID ownerId, CreateCommentRequest req) {
        Resume resume = resumeService.getMyResume(resumeId);

        if (!resume.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You do not own this resume");
        }

        ResumeVersion version = resumeVersionRepo.findByIdAndResume_Id(versionId, resumeId)
                .orElseThrow(VersionNotFoundException::new);

        Comment c = new Comment();
        c.setResumeVersion(version);

        User authorRef = userRepo.getReferenceById(ownerId);
        c.setAuthorUser(authorRef);
        c.setAuthorLabel("Owner");
        c.setBody(req.body());
        c.setAnchorRef(req.anchorRef());

        if (req.parentCommentId() != null) {
            Comment parent = commentRepo.findById(req.parentCommentId())
                    .orElseThrow(() -> new IllegalArgumentException("PARENT_COMMENT_NOT_FOUND"));

            if (parent.getResumeVersion() == null ||
                    !parent.getResumeVersion().getId().equals(version.getId())) {
                throw new IllegalArgumentException("PARENT_NOT_SAME_VERSION");
            }

            c.setParentComment(parent);
        }

        c.setCreatedAt(Instant.now(clock));
        return commentRepo.save(c);
    }

    @Transactional
    public List<Comment> listOwner(UUID resumeId, UUID versionId) {
        resumeService.getMyResume(resumeId);

        resumeVersionRepo.findByIdAndResume_Id(versionId, resumeId)
                .orElseThrow(() -> new IllegalArgumentException("VERSION_NOT_FOUND"));

        return commentRepo.findByResumeVersion_IdOrderByCreatedAtAsc(versionId);
    }

    @Transactional
    public void deleteOwner(UUID resumeId, UUID versionId, UUID commentId) {
        resumeService.getMyResume(resumeId);

        resumeVersionRepo.findByIdAndResume_Id(versionId, resumeId)
                .orElseThrow(() -> new IllegalArgumentException("VERSION_NOT_FOUND"));

        Comment comment = commentRepo.findByIdAndResumeVersion_Id(commentId, versionId)
                .orElseThrow(() -> new IllegalArgumentException("COMMENT_NOT_FOUND"));

        deleteCommentThread(comment);
    }

    @Transactional
    public List<Comment> listPublic(String token, String ip, String ua) {
        ShareLink link = shareLinkService.resolveValidLinkOrThrow(token, ip, ua);

        ResumeVersion current = link.getResume().getCurrentVersion();
        if (current == null) {
            shareLinkService.auditComment(
                    link,
                    AccessAudit.EventType.COMMENT_DENIED,
                    ip,
                    ua,
                    false,
                    "no_current_version",
                    null
            );
            throw new IllegalArgumentException("NO_CURRENT_VERSION");
        }

        return commentRepo.findByResumeVersion_IdOrderByCreatedAtAsc(current.getId());
    }

    @Transactional
    public Comment createPublic(String token, String ip, String ua, CreateCommentRequest req) {
        ShareLink link = shareLinkService.resolveValidLinkOrThrow(token, ip, ua);

        ResumeVersion current = link.getResume().getCurrentVersion();
        if (current == null) {
            shareLinkService.auditComment(
                    link,
                    AccessAudit.EventType.COMMENT_DENIED,
                    ip,
                    ua,
                    false,
                    "no_current_version",
                    null
            );
            throw new IllegalArgumentException("NO_CURRENT_VERSION");
        }

        shareLinkService.auditComment(
                link,
                AccessAudit.EventType.COMMENT_ATTEMPT,
                ip,
                ua,
                true,
                null,
                current
        );

        if (link.getPermission() != ShareLink.Permission.COMMENT) {
            shareLinkService.auditComment(
                    link,
                    AccessAudit.EventType.COMMENT_DENIED,
                    ip,
                    ua,
                    false,
                    "permission_denied",
                    current
            );
            throw new ForbiddenException("Share link does not allow commenting");
        }

        String label = (req.guestLabel() == null || req.guestLabel().isBlank())
                ? "Guest"
                : req.guestLabel().trim();

        Comment c = new Comment();
        c.setResumeVersion(current);
        c.setAuthorUser(null);
        c.setAuthorLabel(label);
        c.setBody(req.body());
        c.setAnchorRef(req.anchorRef());

        if (req.parentCommentId() != null) {
            Comment parent = commentRepo.findById(req.parentCommentId())
                    .orElseThrow(() -> {
                        shareLinkService.auditComment(
                                link,
                                AccessAudit.EventType.COMMENT_DENIED,
                                ip,
                                ua,
                                false,
                                "parent_not_found",
                                current
                        );
                        return new IllegalArgumentException("PARENT_COMMENT_NOT_FOUND");
                    });

            if (parent.getResumeVersion() == null ||
                    !parent.getResumeVersion().getId().equals(current.getId())) {
                shareLinkService.auditComment(
                        link,
                        AccessAudit.EventType.COMMENT_DENIED,
                        ip,
                        ua,
                        false,
                        "parent_not_same_version",
                        current
                );
                throw new IllegalArgumentException("PARENT_NOT_SAME_VERSION");
            }

            c.setParentComment(parent);
        }

        c.setCreatedAt(Instant.now(clock));
        Comment saved = commentRepo.save(c);

        shareLinkService.auditComment(
                link,
                AccessAudit.EventType.COMMENT_CREATED,
                ip,
                ua,
                true,
                null,
                current
        );

        return saved;
    }

    private void deleteCommentThread(Comment root) {
        List<Comment> children = commentRepo.findByParentComment_Id(root.getId());
        for (Comment child : children) {
            deleteCommentThread(child);
        }
        commentRepo.delete(root);
    }
}
