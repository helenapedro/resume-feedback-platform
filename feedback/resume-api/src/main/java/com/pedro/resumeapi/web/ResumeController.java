package com.pedro.resumeapi.web;

import com.pedro.resumeapi.domain.Resume;
import com.pedro.resumeapi.domain.ResumeVersion;
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
    public List<Resume> list() {
        return resumeService.listMyResumes();
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable UUID id) {
        Resume r = resumeService.getMyResume(id);
        List<ResumeVersion> versions = resumeService.listVersions(id);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("resume", r);
        out.put("versions", versions);
        return out;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Resume create(@RequestParam(required = false) String title,
                         @RequestPart("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File is required");
        return resumeService.createResume(title, file);
    }

    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResumeVersion addVersion(@PathVariable UUID id,
                                    @RequestPart("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File is required");
        return resumeService.addVersion(id, file);
    }
}
