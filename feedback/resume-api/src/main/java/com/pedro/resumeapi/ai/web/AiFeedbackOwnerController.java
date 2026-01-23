package com.pedro.resumeapi.ai.web;

import com.pedro.resumeapi.ai.dto.AiFeedbackDTO;
import com.pedro.resumeapi.ai.service.AiFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resumes/{resumeId}/versions/{versionId}/ai-feedback")
public class AiFeedbackOwnerController {

    private final AiFeedbackService aiFeedbackService;

    @GetMapping
    public AiFeedbackDTO getLatest(
            @PathVariable UUID resumeId,
            @PathVariable UUID versionId
    ) {
        return aiFeedbackService.getLatestFeedback(resumeId, versionId);
    }
}
