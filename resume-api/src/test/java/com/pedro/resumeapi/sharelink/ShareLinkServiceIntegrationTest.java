package com.pedro.resumeapi.sharelink;

import com.pedro.resumeapi.accessaudit.domain.AccessAudit;
import com.pedro.resumeapi.accessaudit.repository.AccessAuditRepository;
import com.pedro.resumeapi.api.error.ShareLinkExpiredException;
import com.pedro.resumeapi.api.error.ShareLinkRevokedException;
import com.pedro.resumeapi.resume.domain.Resume;
import com.pedro.resumeapi.resume.domain.ResumeVersion;
import com.pedro.resumeapi.resume.repository.ResumeRepository;
import com.pedro.resumeapi.resume.repository.ResumeVersionRepository;
import com.pedro.resumeapi.sharelink.domain.ShareLink;
import com.pedro.resumeapi.sharelink.repository.ShareLinkRepository;
import com.pedro.resumeapi.sharelink.service.ShareLinkService;
import com.pedro.resumeapi.user.domain.User;
import com.pedro.resumeapi.user.dto.UserPrincipal;
import com.pedro.resumeapi.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class ShareLinkServiceIntegrationTest {

    @jakarta.annotation.Resource
    private ShareLinkService shareLinkService;
    @jakarta.annotation.Resource
    private ShareLinkRepository shareLinkRepository;
    @jakarta.annotation.Resource
    private AccessAuditRepository accessAuditRepository;
    @jakarta.annotation.Resource
    private ResumeRepository resumeRepository;
    @jakarta.annotation.Resource
    private ResumeVersionRepository resumeVersionRepository;
    @jakarta.annotation.Resource
    private UserRepository userRepository;

    private User owner;
    private Resume resume;
    private ResumeVersion version;

    @BeforeEach
    void setUp() {
        accessAuditRepository.deleteAll();
        shareLinkRepository.deleteAll();
        resumeRepository.findAll().forEach(existingResume -> {
            existingResume.setCurrentVersion(null);
            resumeRepository.save(existingResume);
        });
        resumeRepository.deleteAll();
        resumeVersionRepository.deleteAll();
        userRepository.deleteAll();

        owner = new User();
        owner.setEmail("owner@example.com");
        owner.setPasswordHash("encoded");
        owner.setRole(User.Role.USER);
        owner.setEnabled(true);
        userRepository.save(owner);

        resume = new Resume();
        resume.setOwner(owner);
        resume.setTitle("Backend Resume");
        resumeRepository.save(resume);

        version = new ResumeVersion();
        version.setResume(resume);
        version.setVersionNumber(1);
        version.setOriginalFilename("resume.pdf");
        version.setFileName("resume.pdf");
        version.setContentType("application/pdf");
        version.setStorageKey("local/path/resume.pdf");
        resumeVersionRepository.save(version);

        resume.setCurrentVersion(version);
        resumeRepository.save(resume);

        authenticate(owner.getId());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolveValidLinkIncrementsUseCountAndCreatesSuccessAudit() {
        var created = shareLinkService.create(
                resume.getId(),
                ShareLink.Permission.VIEW,
                Instant.now().plusSeconds(3600),
                3,
                owner.getId());

        ShareLink resolved = shareLinkService.resolveValidLinkOrThrow(created.token(), "127.0.0.1", "JUnit");

        assertEquals(resume.getId(), resolved.getResume().getId());
        assertEquals(1, shareLinkRepository.findById(created.id()).orElseThrow().getUseCount());

        List<AccessAudit> audits = accessAuditRepository.findAll();
        assertEquals(1, audits.size());
        assertEquals(AccessAudit.EventType.OPEN_LINK, audits.get(0).getEventType());
        assertEquals(true, audits.get(0).isSuccess());
    }

    @Test
    void resolveRevokedLinkThrowsAndAuditsFailure() {
        var created = shareLinkService.create(
                resume.getId(),
                ShareLink.Permission.COMMENT,
                Instant.now().plusSeconds(3600),
                null,
                owner.getId());

        ShareLink link = shareLinkRepository.findById(created.id()).orElseThrow();
        link.setRevokedAt(Instant.now());
        shareLinkRepository.save(link);

        assertThrows(ShareLinkRevokedException.class,
                () -> shareLinkService.resolveValidLinkOrThrow(created.token(), "127.0.0.1", "JUnit"));

        AccessAudit audit = accessAuditRepository.findAll().get(0);
        assertEquals(false, audit.isSuccess());
        assertEquals("revoked", audit.getFailureReason());
    }

    @Test
    void resolveExpiredLinkThrowsAndAuditsFailure() {
        var created = shareLinkService.create(
                resume.getId(),
                ShareLink.Permission.VIEW,
                Instant.now().minusSeconds(10),
                null,
                owner.getId());

        assertThrows(ShareLinkExpiredException.class,
                () -> shareLinkService.resolveValidLinkOrThrow(created.token(), "127.0.0.1", "JUnit"));

        AccessAudit audit = accessAuditRepository.findAll().get(0);
        assertNotNull(audit.getOccurredAt());
        assertEquals(false, audit.isSuccess());
        assertEquals("expired", audit.getFailureReason());
    }

    private void authenticate(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(new UserPrincipal(userId), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
