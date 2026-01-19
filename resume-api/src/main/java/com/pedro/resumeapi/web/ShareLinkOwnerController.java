package com.pedro.resumeapi.web;

import com.pedro.resumeapi.domain.User;
import com.pedro.resumeapi.dto.CreateShareLinkRequest;
import com.pedro.resumeapi.dto.CreateShareLinkResponse;
import com.pedro.resumeapi.dto.ShareLinkDTO;
import com.pedro.resumeapi.mapper.ShareLinkMapper;
import com.pedro.resumeapi.repository.ShareLinkRepository;
import com.pedro.resumeapi.repository.UserRepository;
import com.pedro.resumeapi.security.CurrentUser;
import com.pedro.resumeapi.service.ShareLinkService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/resumes/{resumeId}/share-links")
public class ShareLinkOwnerController {

    private final ShareLinkService shareLinkService;
    private final ShareLinkRepository shareLinkRepo;

    private final CurrentUser currentUser;
    private final UserRepository userRepository;

    private User currentUserEntity() {
        UUID userId = currentUser.id();
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    @PostMapping
    public CreateShareLinkResponse create(@PathVariable UUID resumeId,
                                          @RequestBody CreateShareLinkRequest req) {

        User owner = currentUserEntity();

        var result = shareLinkService.create(
                resumeId,
                req.permission(),
                req.expiresAt(),
                req.maxUses()
        );

        return new CreateShareLinkResponse(
                result.id(),
                result.token(),
                result.permission(),
                result.expiresAt(),
                result.maxUses()
        );
    }

    @GetMapping
    public List<ShareLinkDTO> list(@PathVariable UUID resumeId) {
        return shareLinkService.listForOwner(resumeId)
                .stream()
                .map(ShareLinkMapper::toDTO)
                .toList();
    }

    @PostMapping("/{linkId}/revoke")
    public void revoke(@PathVariable UUID resumeId, @PathVariable UUID linkId) {
        shareLinkService.revoke(resumeId, linkId);
    }
}
