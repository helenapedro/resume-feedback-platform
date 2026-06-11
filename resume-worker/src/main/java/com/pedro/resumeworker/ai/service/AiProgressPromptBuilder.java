package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.common.ai.Language;
import com.pedro.common.ai.mongo.AiFeedbackDocument;
import com.pedro.resumeworker.ai.foundryiq.FoundryIqGroundingProvider;
import com.pedro.resumeworker.ai.domain.ResumeVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiProgressPromptBuilder {

    private final int maxResumeChars;
    private final int maxProgressResumeChars;
    private final String englishTemplate;
    private final String portugueseTemplate;
    private final FoundryIqGroundingProvider groundingProvider;

    public AiProgressPromptBuilder(int maxResumeChars, PromptTemplateLoader promptTemplateLoader) {
        this(maxResumeChars, promptTemplateLoader, FoundryIqGroundingProvider.NONE);
    }

    @Autowired
    public AiProgressPromptBuilder(
            @Value("${app.ai-feedback.max-resume-chars:12000}") int maxResumeChars,
            PromptTemplateLoader promptTemplateLoader,
            FoundryIqGroundingProvider groundingProvider) {
        this.maxResumeChars = maxResumeChars;
        this.maxProgressResumeChars = Math.max(1500, maxResumeChars / 2);
        this.englishTemplate = promptTemplateLoader.load("prompts/ai-progress-en.md");
        this.portugueseTemplate = promptTemplateLoader.load("prompts/ai-progress-pt.md");
        this.groundingProvider = groundingProvider;
    }

    public String build(
            AiJobRequestedMessage message,
            ResumeVersion currentVersion,
            ResumeVersion previousVersion,
            String currentResumeText,
            String previousResumeText,
            AiFeedbackDocument previousFeedback,
            Language language) {
        Language resolvedLanguage = language == Language.PT ? Language.PT : Language.EN;
        return templateFor(resolvedLanguage)
                .replace("{{JOB_ID}}", String.valueOf(message.jobId()))
                .replace("{{RESUME_ID}}", String.valueOf(message.resumeId()))
                .replace("{{CURRENT_RESUME_VERSION_ID}}", String.valueOf(currentVersion.getId()))
                .replace("{{BASELINE_RESUME_VERSION_ID}}", String.valueOf(previousVersion.getId()))
                .replace("{{OWNER_ID}}", String.valueOf(message.ownerId()))
                .replace("{{MAX_RESUME_CHARS}}", String.valueOf(maxResumeChars))
                .replace("{{MAX_PROGRESS_RESUME_CHARS}}", String.valueOf(maxProgressResumeChars))
                .replace("{{MICROSOFT_IQ_GROUNDING_SECTION}}",
                        groundingProvider.progressGrounding(
                                resolvedLanguage,
                                currentResumeText,
                                previousResumeText,
                                previousFeedback))
                .replace("{{PREVIOUS_RESUME_TEXT}}", buildResumeExcerpt(previousResumeText))
                .replace("{{PREVIOUS_FEEDBACK_SECTION}}", buildPreviousFeedbackSection(previousFeedback, resolvedLanguage))
                .replace("{{CURRENT_RESUME_TEXT}}", buildResumeExcerpt(currentResumeText));
    }

    private String templateFor(Language language) {
        return language == Language.PT ? portugueseTemplate : englishTemplate;
    }

    private String buildPreviousFeedbackSection(AiFeedbackDocument previousFeedback, Language language) {
        if (previousFeedback == null) {
            return language == Language.PT ? "Feedback anterior: INDISPONIVEL" : "Previous feedback: NOT AVAILABLE";
        }
        return """
                %s:
                - summary: %s
                - strengths: %s
                - improvements: %s
                """.formatted(
                language == Language.PT ? "Feedback anterior entregue ao utilizador" : "Previous feedback delivered to the user",
                sanitize(previousFeedback.getSummary()),
                sanitizeList(previousFeedback.getStrengths()),
                sanitizeList(previousFeedback.getImprovements()));
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "NOT AVAILABLE";
        }
        return value.trim();
    }

    private String buildResumeExcerpt(String value) {
        String sanitized = sanitize(value);
        if ("NOT AVAILABLE".equals(sanitized) || sanitized.length() <= maxProgressResumeChars) {
            return sanitized;
        }

        int headChars = Math.max(750, maxProgressResumeChars * 2 / 3);
        int tailChars = maxProgressResumeChars - headChars;
        String head = sanitized.substring(0, headChars).trim();
        String tail = sanitized.substring(sanitized.length() - tailChars).trim();
        return """
                %s
                [TRUNCATED MIDDLE: resume excerpt limited for progress analysis]
                %s
                """.formatted(head, tail);
    }

    private String sanitizeList(List<String> values) {
        return values == null || values.isEmpty() ? "[]" : values.toString();
    }
}
