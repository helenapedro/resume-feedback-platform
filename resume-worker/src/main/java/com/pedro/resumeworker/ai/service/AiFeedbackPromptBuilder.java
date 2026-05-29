package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.common.ai.Language;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AiFeedbackPromptBuilder {

    private final int maxResumeChars;
    private final String englishTemplate;

    public AiFeedbackPromptBuilder(
            @Value("${app.ai-feedback.max-resume-chars:12000}") int maxResumeChars,
            PromptTemplateLoader promptTemplateLoader) {
        this.maxResumeChars = maxResumeChars;
        this.englishTemplate = promptTemplateLoader.load("prompts/ai-feedback-en.md");
    }

    public String build(AiJobRequestedMessage message, String resumeText, Language language) {
        return englishTemplate
                .replace("{{JOB_ID}}", String.valueOf(message.jobId()))
                .replace("{{RESUME_ID}}", String.valueOf(message.resumeId()))
                .replace("{{RESUME_VERSION_ID}}", String.valueOf(message.resumeVersionId()))
                .replace("{{OWNER_ID}}", String.valueOf(message.ownerId()))
                .replace("{{MAX_RESUME_CHARS}}", String.valueOf(maxResumeChars))
                .replace("{{RESUME_CONTENT_SECTION}}", buildContentSection(resumeText));
    }

    private String buildContentSection(String resumeText) {
        if (resumeText == null || resumeText.isBlank()) {
            return "Resume extracted: NOT AVAILABLE";
        }
        String boundedResumeText = truncate(resumeText);
        return """
                Resume extracted (raw text):
                %s
                """.formatted(boundedResumeText);
    }

    private String truncate(String value) {
        String trimmed = value.trim();
        if (trimmed.length() <= maxResumeChars) {
            return trimmed;
        }
        return trimmed.substring(0, maxResumeChars)
                + "\n[TRUNCATED: resume text exceeded configured analysis limit]";
    }
}
