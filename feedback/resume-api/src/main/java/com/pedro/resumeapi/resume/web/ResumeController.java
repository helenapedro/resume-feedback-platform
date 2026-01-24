package com.pedro.resumeapi.resume.web;

import com.pedro.resumeapi.api.error.FileRequiredException;
import com.pedro.resumeapi.resume.dto.ResumeSummaryDTO;
import com.pedro.resumeapi.resume.dto.ResumeVersionDTO;
import com.pedro.resumeapi.resume.mapper.ResumeMapper;
import com.pedro.resumeapi.resume.service.ResumeService;
import com.pedro.resumeapi.resume.service.ResumeStorageService;
import lombok.AllArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.core.io.Resource;
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
    private final ResumeStorageService resumeStorageService;
    private final ResumeMapper resumeMapper;

    @GetMapping
    public List<ResumeSummaryDTO> list() {
        return resumeService.listMyResumes()
                .stream()
                .map(resumeMapper::toSummaryDTO)
                .toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable UUID id) {
        var resume = resumeService.getMyResume(id);
        var versions = resumeService.listVersions(id);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("resume", resumeMapper.toSummaryDTO(resume));

        out.put("versions", versions
                .stream()
                .map(resumeMapper::toVersionDTO)
                .toList()
        );

        return out;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResumeSummaryDTO create(@RequestParam(required = false) String title,
                                   @RequestPart("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty())
            throw new FileRequiredException();

        var resume = resumeService.createResume(title, file);
        return resumeMapper.toSummaryDTO(resume);
    }

    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResumeVersionDTO addVersion(@PathVariable UUID id,
                                       @RequestPart("file") MultipartFile file) throws IOException {

        if (file == null || file.isEmpty())
            throw new BadRequestException("File is required");

        var version = resumeService.addVersion(id, file);
        return resumeMapper.toVersionDTO(version);
    }

    @GetMapping("/{resumeId}/versions/{versionId}/download")
    public ResponseEntity<Resource> download(
            @PathVariable UUID resumeId,
            @PathVariable UUID versionId
    ) {
        var payload = resumeStorageService.downloadVersionOwner(resumeId, versionId);

        if (payload.isPresigned()) {
            return ResponseEntity.status(302)
                    .header(org.springframework.http.HttpHeaders.LOCATION, payload.presignedUrl())
                    .build();
        }

        String safeName = payload.filename()
                .replace("\"", "")
                .replace("\r", "")
                .replace("\n", "")
                .replace("\\", "_")
                .replace("/", "_");

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(payload.contentType()))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + safeName + "\"")
                .body(payload.resource());
    }
}
