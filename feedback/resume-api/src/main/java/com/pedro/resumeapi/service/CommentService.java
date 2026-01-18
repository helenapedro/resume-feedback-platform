package com.pedro.resumeapi.service;

import com.pedro.resumeapi.api.error.VersionNotFoundException;
import com.pedro.resumeapi.domain.*;
import com.pedro.resumeapi.repository.CommentRepository;
import com.pedro.resumeapi.repository.ResumeVersionRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
public class CommentService {

    private final CommentRepository commentRepo;
    private final ResumeVersionRepository versionRepo;
    private final ShareLinkService shareLinkService;

    @Transactional
    public Comment addOwnerComment(UUID resumeId, UUID versionId, String body, String anchorRef,
                                   UUID parentId, User owner) {
        var resumeVersion = versionRepo.findById(versionId)
                .orElseThrow(VersionNotFoundException::new);
        if (!resumeVersion.getResume().getId().equals(resumeId))
            throw new IllegalArgumentException("Version not in resume");

        var comment = new Comment();
        comment.setResumeVersion(resumeVersion);
        comment.setAuthorUser(owner);
        comment.setAuthorLabel(owner.getEmail());
        comment.setBody(body);
        comment.setAnchorRef(anchorRef);

        if (parentId != null) {
            Comment parent = commentRepo.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent not found"));
            comment.setParentComment(parent);
        }

        return commentRepo.save(comment);
    }

    @Transactional
    public Comment addGuestComment(String token, UUID resumeVersionId, String body,
                                   String anchorRef, String authorLabel, String ip, String ua) {
        ShareLink link = shareLinkService.resolveValidLinkOrThrow(token, ip, ua);

        if (link.getPermission() != ShareLink.Permission.COMMENT) {
            throw new IllegalArgumentException("Permission denied");
        }

        var resumeVersion = versionRepo.findById(resumeVersionId)
                .orElseThrow(VersionNotFoundException::new);

        if (!resumeVersion.getResume().getId().equals(link.getResume().getId())) {
            throw new IllegalArgumentException("Version does not belong to resume");
        }

        var comment = new Comment();
        comment.setResumeVersion(resumeVersion);
        comment.setAuthorUser(null);
        comment.setAuthorLabel((authorLabel == null ||
                authorLabel.isBlank()) ? "Guest" : authorLabel);
        comment.setBody(body);
        comment.setAnchorRef(anchorRef);

        return commentRepo.save(comment);
    }
}
