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
        String template = language == Language.PT ? portugueseTemplate : englishTemplate;
        return template
                .replace("{{JOB_ID}}", String.valueOf(message.jobId()))
                .replace("{{RESUME_ID}}", String.valueOf(message.resumeId()))
                .replace("{{RESUME_VERSION_ID}}", String.valueOf(message.resumeVersionId()))
                .replace("{{OWNER_ID}}", String.valueOf(message.ownerId()))
                .replace("{{MAX_RESUME_CHARS}}", String.valueOf(maxResumeChars))
                .replace("{{RESUME_CONTENT_SECTION}}", buildContentSection(resumeText, language));
    }

    private String buildContentSection(String resumeText, Language language) {
        if (resumeText == null || resumeText.isBlank()) {
            return language == Language.PT
                    ? "Curriculo extraido: NAO DISPONIVEL"
                    : "Resume extracted: NOT AVAILABLE";
        }
        return language == Language.PT
                ? """
                Curriculo extraido (texto bruto):
                %s
                """.formatted(resumeText)
                : """
                Resume extracted (raw text):
                %s
                """.formatted(resumeText);
    }
}
