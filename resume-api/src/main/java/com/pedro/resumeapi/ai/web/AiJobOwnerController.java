package com.pedro.resumeapi.ai.web;

import com.pedro.common.ai.Language;
import com.pedro.resumeapi.ai.dto.AiJobDTO;
import com.pedro.resumeapi.ai.mapper.AiJobMapper;
import com.pedro.resumeapi.ai.service.AiJobService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/resumes/{resumeId}/versions/{versionId}/ai-jobs")
public class AiJobOwnerController {

    private final AiJobService aiJobService;

    @GetMapping("/latest")
    public AiJobDTO getLatest(
            @PathVariable UUID resumeId,
            @PathVariable UUID versionId) {
        return AiJobMapper.toDTO(aiJobService.getLatestJobForVersion(resumeId, versionId));
    }

    @PostMapping("/regenerate")
    public AiJobDTO regenerate(
            @PathVariable UUID resumeId,
            @PathVariable UUID versionId,
            @RequestParam(name = "language", defaultValue = "EN") Language language) {
        return AiJobMapper.toDTO(aiJobService.regenerateForVersion(resumeId, versionId, language));
    }
}
