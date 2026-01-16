package com.pedro.resumeapi.web;

import com.pedro.resumeapi.domain.User;
import com.pedro.resumeapi.dto.ResumeSummaryDTO;
import com.pedro.resumeapi.dto.ResumeVersionDTO;
import com.pedro.resumeapi.service.ResumeService;
import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/resumes")
@AllArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping
    public List<ResumeSummaryDTO> list() {
        return resumeService.listMyResumes().stream().map(resumeService::toSummaryDTO).toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable UUID id) {
        var resume = resumeService.getMyResume(id);
        var versions = resumeService.listVersions(id);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("resume", resumeService.toSummaryDTO(resume));

        out.put("versions", versions.stream().map(resumeService::toVersionDTO).toList());

        return out;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResumeSummaryDTO create(@RequestParam(required = false) String title,
                                   @RequestPart("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File is required");

        var resume = resumeService.createResume(title, file);
        return resumeService.toSummaryDTO(resume);
    }

    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResumeVersionDTO addVersion(@PathVariable UUID id,
                                       @RequestPart("file") MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File is required");

        var version = resumeService.addVersion(id, file);
        return resumeService.toVersionDTO(version);
    }

    @GetMapping("/{resumeId}/versions/{versionId}/download")
    public ResponseEntity<Resource> download(
            @PathVariable UUID resumeId,
            @PathVariable UUID versionId
    ) {
        var payload = resumeService.downloadVersion(resumeId, versionId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + payload.filename().replace("\"", "") + "\"")
                .header(HttpHeaders.CONTENT_TYPE, payload.contentType())
                .body(payload.resource());
    }
}
