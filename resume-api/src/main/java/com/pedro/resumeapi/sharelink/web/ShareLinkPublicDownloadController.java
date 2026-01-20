package com.pedro.resumeapi.sharelink.web;

import com.pedro.resumeapi.resume.domain.Resume;
import com.pedro.resumeapi.resume.domain.ResumeVersion;
import com.pedro.resumeapi.resume.service.ResumeStorageService;
import com.pedro.resumeapi.sharelink.service.ShareLinkService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/share")
public class ShareLinkPublicDownloadController {

    private final ShareLinkService shareLinkService;
    private final ResumeStorageService resumeStorageService;

    @GetMapping("/{token}/download")
    public ResponseEntity<Resource> download(@PathVariable String token, HttpServletRequest request) {

        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");

        var link = shareLinkService.resolveValidLinkOrThrow(token, ip, ua);

        Resume resume = link.getResume();
        ResumeVersion current = resume.getCurrentVersion();

        if (current == null) {
            shareLinkService.auditDownload(link, ip, ua, false, "no_current_version", null);
            throw new IllegalArgumentException("NO_CURRENT_VERSION");
        }

        var payload = resumeStorageService.downloadVersionPublic(resume.getId(), current.getId());

        shareLinkService.auditDownload(link, ip, ua, true, null, current);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + payload.filename().replace("\"", "") + "\"")
                .header(HttpHeaders.CONTENT_TYPE, payload.contentType())
                .body(payload.resource());
    }
}
