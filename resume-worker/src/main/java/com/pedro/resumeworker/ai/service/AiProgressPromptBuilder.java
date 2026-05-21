package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.common.ai.Language;
import com.pedro.common.ai.mongo.AiFeedbackDocument;
import com.pedro.resumeworker.ai.domain.ResumeVersion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiProgressPromptBuilder {

    private final int maxResumeChars;
    private final String englishTemplate;

    public AiProgressPromptBuilder(
            @Value("${app.ai-feedback.max-resume-chars:12000}") int maxResumeChars,
            PromptTemplateLoader promptTemplateLoader) {
        this.maxResumeChars = maxResumeChars;
        this.englishTemplate = promptTemplateLoader.load("prompts/ai-progress-en.md");
    }

    public String build(
            AiJobRequestedMessage message,
            ResumeVersion currentVersion,
            ResumeVersion previousVersion,
            String currentResumeText,
            String previousResumeText,
            AiFeedbackDocument previousFeedback,
            Language language) {
        return englishTemplate
                .replace("{{JOB_ID}}", String.valueOf(message.jobId()))
                .replace("{{RESUME_ID}}", String.valueOf(message.resumeId()))
                .replace("{{CURRENT_RESUME_VERSION_ID}}", String.valueOf(currentVersion.getId()))
                .replace("{{BASELINE_RESUME_VERSION_ID}}", String.valueOf(previousVersion.getId()))
                .replace("{{OWNER_ID}}", String.valueOf(message.ownerId()))
                .replace("{{MAX_RESUME_CHARS}}", String.valueOf(maxResumeChars))
                .replace("{{PREVIOUS_RESUME_TEXT}}", sanitize(previousResumeText))
                .replace("{{PREVIOUS_FEEDBACK_SECTION}}", buildPreviousFeedbackSection(previousFeedback))
                .replace("{{CURRENT_RESUME_TEXT}}", sanitize(currentResumeText));
    }

    private String buildPreviousFeedbackSection(AiFeedbackDocument previousFeedback) {
        if (previousFeedback == null) {
            return "Previous feedback: NOT AVAILABLE";
        }
        return """
                %s:
                - summary: %s
                - strengths: %s
                - improvements: %s
                """.formatted(
                "Previous feedback delivered to the user",
                sanitize(previousFeedback.getSummary()),
                sanitizeList(previousFeedback.getStrengths()),
                sanitizeList(previousFeedback.getImprovements()));
    }

    private String sanitize(String value) {
        return value == null || value.isBlank()
                ? "NOT AVAILABLE"
                : value;
    }

    private String sanitizeList(List<String> values) {
        return values == null || values.isEmpty() ? "[]" : values.toString();
    }
}
