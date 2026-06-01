package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.common.ai.Language;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AiFeedbackPromptBuilder {

    private final int maxResumeChars;
    private final String englishTemplate;
    private final String portugueseTemplate;

    public AiFeedbackPromptBuilder(
            @Value("${app.ai-feedback.max-resume-chars:12000}") int maxResumeChars,
            PromptTemplateLoader promptTemplateLoader) {
        this.maxResumeChars = maxResumeChars;
        this.englishTemplate = promptTemplateLoader.load("prompts/ai-feedback-en.md");
        this.portugueseTemplate = promptTemplateLoader.load("prompts/ai-feedback-pt.md");
    }

    public String build(AiJobRequestedMessage message, String resumeText, Language language) {
        Language resolvedLanguage = language == Language.PT ? Language.PT : Language.EN;
        return templateFor(resolvedLanguage)
                .replace("{{JOB_ID}}", String.valueOf(message.jobId()))
                .replace("{{RESUME_ID}}", String.valueOf(message.resumeId()))
                .replace("{{RESUME_VERSION_ID}}", String.valueOf(message.resumeVersionId()))
                .replace("{{OWNER_ID}}", String.valueOf(message.ownerId()))
                .replace("{{MAX_RESUME_CHARS}}", String.valueOf(maxResumeChars))
                .replace("{{RESUME_CONTENT_SECTION}}", buildContentSection(resumeText, resolvedLanguage));
    }

    private String templateFor(Language language) {
        return language == Language.PT ? portugueseTemplate : englishTemplate;
    }

    private String buildContentSection(String resumeText, Language language) {
        if (resumeText == null || resumeText.isBlank()) {
            return language == Language.PT ? "Curriculo extraido: INDISPONIVEL" : "Resume extracted: NOT AVAILABLE";
        }
        String boundedResumeText = truncate(resumeText);
        String label = language == Language.PT ? "Curriculo extraido (texto bruto):" : "Resume extracted (raw text):";
        return """
                %s
                %s
                """.formatted(label, boundedResumeText);
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
