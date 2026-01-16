package com.pedro.resumeapi.web;

import com.pedro.resumeapi.domain.Resume;
import com.pedro.resumeapi.domain.ResumeVersion;
import com.pedro.resumeapi.dto.ResumeSummaryDTO;
import com.pedro.resumeapi.dto.ResumeVersionDTO;
import com.pedro.resumeapi.service.ResumeService;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
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
        return resumeService.listMyResumes().stream()
                .map(r -> new ResumeSummaryDTO(
                        r.getId(),
                        r.getTitle(),
                        r.getCurrentVersion() != null ? r.getCurrentVersion().getId() : null,
                        r.getCreatedAt()
                ))
                .toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable UUID id) {
        Resume r = resumeService.getMyResume(id);
        List<ResumeVersion> versions = resumeService.listVersions(id);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("resume", new ResumeSummaryDTO(
                r.getId(),
                r.getTitle(),
                r.getCurrentVersion() != null ? r.getCurrentVersion().getId() : null,
                r.getCreatedAt()
        ));

        out.put("versions", versions.stream()
                .map(v -> new ResumeVersionDTO(
                        v.getId(),
                        v.getVersionNumber(),
                        v.getOriginalFilename(),
                        v.getContentType(),
                        v.getCreatedAt()
                ))
                .toList()
        );

        return out;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResumeSummaryDTO create(@RequestParam(required = false) String title,
                                   @RequestPart("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File is required");

        Resume resume = resumeService.createResume(title, file);
        return new ResumeSummaryDTO(
                resume.getId(),
                resume.getTitle(),
                resume.getCurrentVersion() != null ? resume.getCurrentVersion().getId() : null,
                resume.getCreatedAt()
        );
    }

    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResumeVersionDTO addVersion(@PathVariable UUID id,
                                       @RequestPart("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File is required");

        ResumeVersion resumeVersion = resumeService.addVersion(id, file);
        return new ResumeVersionDTO(
                resumeVersion.getId(),
                resumeVersion.getVersionNumber(),
                resumeVersion.getOriginalFilename(),
                resumeVersion.getContentType(),
                resumeVersion.getCreatedAt()
        );
    }
}
