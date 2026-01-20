package com.pedro.resumeapi.sharelink.web;

import com.pedro.resumeapi.sharelink.dto.CreateShareLinkRequest;
import com.pedro.resumeapi.sharelink.dto.CreateShareLinkResponse;
import com.pedro.resumeapi.sharelink.dto.ShareLinkDTO;
import com.pedro.resumeapi.sharelink.mapper.ShareLinkMapper;
import com.pedro.resumeapi.security.CurrentUser;
import com.pedro.resumeapi.sharelink.service.ShareLinkService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/resumes/{resumeId}/share-links")
public class ShareLinkOwnerController {

    private final ShareLinkService shareLinkService;
    private final CurrentUser currentUser;

    @PostMapping
    public CreateShareLinkResponse create(@PathVariable UUID resumeId,
                                          @RequestBody CreateShareLinkRequest req) {

        UUID ownerId = currentUser.id();

        var result = shareLinkService.create(
                resumeId,
                req.permission(),
                req.expiresAt(),
                req.maxUses(),
                ownerId
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
        return shareLinkService.listForOwner(resumeId).stream()
                .map(ShareLinkMapper::toDTO)
                .toList();
    }

    @PostMapping("/{linkId}/revoke")
    public void revoke(@PathVariable UUID resumeId, @PathVariable UUID linkId) {
        UUID ownerId = currentUser.id();
        shareLinkService.revoke(resumeId, linkId, ownerId);
    }
}
