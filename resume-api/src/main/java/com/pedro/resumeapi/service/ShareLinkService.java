package com.pedro.resumeapi.service;

import com.pedro.resumeapi.api.error.ForbiddenException;
import com.pedro.resumeapi.domain.*;
import com.pedro.resumeapi.repository.AccessAuditRepository;
import com.pedro.resumeapi.repository.ShareLinkRepository;
import com.pedro.resumeapi.repository.UserRepository;
import com.pedro.resumeapi.utils.TokenUtil;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ShareLinkService {

    private final ShareLinkRepository shareLinkRepo;
    private final AccessAuditRepository auditRepo;
    private final ResumeService resumeService;
    private final UserRepository userRepository;
    private final Clock clock = Clock.systemUTC();

    @Transactional
    public CreateShareLinkResult create(UUID resumeId,
                                        ShareLink.Permission perm,
                                        Instant expiresAt,
                                        Integer maxUses,
                                        UUID ownerId
    ) {
        Resume resume = resumeService.getMyResume(resumeId);

        String token = TokenUtil.newToken();
        String tokenHash = TokenUtil.sha256Hex(token);

        User ownerRef = userRepository.getReferenceById(resume.getOwner().getId());

        ShareLink link = new ShareLink();
        link.setResume(resume);
        link.setPermission(perm);
        link.setExpiresAt(expiresAt);
        link.setMaxUses(maxUses);
        link.setTokenHash(tokenHash);
        link.setCreatedBy(ownerRef);

        shareLinkRepo.save(link);
        return new CreateShareLinkResult(link.getId(), token, perm, expiresAt, maxUses);
    }

    @Transactional
    public List<ShareLink> listForOwner(UUID resumeId, UUID ownerId) {
        resumeService.getMyResume(resumeId);
        return shareLinkRepo.findByResume_IdOrderByCreatedAtDesc(resumeId);
    }

    @Transactional
    public ShareLink resolveValidLinkOrThrow(String rawToken, String ip, String ua) {
        String hash = TokenUtil.sha256Hex(rawToken);

        ShareLink link = shareLinkRepo.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("SHARE_LINK_INVALID"));

        Instant now = Instant.now(clock);

        if (link.isRevoked()) {
            audit(link, AccessAudit.EventType.OPEN_LINK, ip, ua, false, "revoked", null);
            throw new IllegalArgumentException("SHARE_LINK_REVOKED");
        }

        if (link.isExpired(now)) {
            audit(link, AccessAudit.EventType.OPEN_LINK, ip, ua, false, "expired", null);
            throw new IllegalArgumentException("SHARE_LINK_EXPIRED");
        }

        if (link.isExhausted()) {
            audit(link, AccessAudit.EventType.OPEN_LINK, ip, ua, false, "max_uses_reached", null);
            throw new IllegalArgumentException("SHARE_LINK_MAX_USES_REACHED");
        }

        link.setUseCount(link.getUseCount() + 1);
        shareLinkRepo.save(link);

        audit(link, AccessAudit.EventType.OPEN_LINK, ip, ua, true, null, null);
        return link;
    }

    @Transactional
    public void revoke(UUID resumeId, UUID linkId, UUID ownerId) {
        Resume resume = resumeService.getMyResume(resumeId);

        ShareLink link = shareLinkRepo.findById(linkId)
                .orElseThrow(() -> new IllegalArgumentException("SHARE_LINK_NOT_FOUND"));

        if (!link.getResume().getId().equals(resume.getId())) {
            throw new ForbiddenException("You are not allowed to revoke this link");
        }

        link.setRevokedAt(Instant.now(clock));
        shareLinkRepo.save(link);
    }

    @Transactional
    public void auditDownload(ShareLink link, String ip, String ua, boolean success,
                              String reason, ResumeVersion version) {
        audit(link, AccessAudit.EventType.DOWNLOAD, ip, ua, success, reason, version);
    }

    private void audit(ShareLink link, AccessAudit.EventType type, String ip, String ua,
                       boolean success, String reason, ResumeVersion version) {
        AccessAudit a = new AccessAudit();
        a.setShareLink(link);
        a.setResume(link.getResume());
        a.setResumeVersion(version);
        a.setEventType(type);
        a.setIpAddress(ip);
        a.setUserAgent(ua);
        a.setSuccess(success);
        a.setFailureReason(reason);
        auditRepo.save(a);
    }

    public record CreateShareLinkResult(
            UUID id,
            String token,
            ShareLink.Permission permission,
            Instant expiresAt,
            Integer maxUses
    ) {}
}
