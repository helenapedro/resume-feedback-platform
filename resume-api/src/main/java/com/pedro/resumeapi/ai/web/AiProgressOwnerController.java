package com.pedro.resumeapi.ai.web;

import com.pedro.resumeapi.ai.dto.AiProgressDTO;
import com.pedro.resumeapi.ai.service.AiProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resumes/{resumeId}/versions/{versionId}/ai-progress")
public class AiProgressOwnerController {

    private final AiProgressService aiProgressService;

    @GetMapping
    public AiProgressDTO getLatest(
            @PathVariable UUID resumeId,
            @PathVariable UUID versionId
    ) {
        return aiProgressService.getLatestProgress(resumeId, versionId);
    }
}
