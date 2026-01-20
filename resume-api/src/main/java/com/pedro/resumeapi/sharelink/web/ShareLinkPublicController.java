package com.pedro.resumeapi.sharelink.web;

import com.pedro.resumeapi.sharelink.dto.ShareLinkPublicDTO;
import com.pedro.resumeapi.sharelink.service.ShareLinkService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/share")
public class ShareLinkPublicController {

    private final ShareLinkService shareLinkService;

    @GetMapping("/{token}")
    public ShareLinkPublicDTO open(@PathVariable String token, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");

        var link = shareLinkService.resolveValidLinkOrThrow(token, ip, ua);

        var resume = link.getResume();
        var current = resume.getCurrentVersion();

        return new ShareLinkPublicDTO(
                resume.getId(),
                current == null ? null : current.getId(),
                link.getPermission(),
                link.getExpiresAt()
        );
    }
}
