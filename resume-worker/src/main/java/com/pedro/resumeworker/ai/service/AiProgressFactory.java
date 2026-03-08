package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.common.ai.mongo.AiFeedbackDocument;
import com.pedro.common.ai.mongo.AiProgressDocument;
import com.pedro.resumeworker.ai.domain.ResumeVersion;
import com.pedro.resumeworker.ai.gemini.GeminiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@Slf4j
public class AiProgressFactory {

    private final String promptVersion;
    private final int maxResumeChars;
    private final GeminiClient geminiClient;
    private final ResumeTextExtractor resumeTextExtractor;

    public AiProgressFactory(
            @Value("${app.ai-feedback.prompt-version:v1}") String promptVersion,
            @Value("${app.ai-feedback.max-resume-chars:12000}") int maxResumeChars,
            GeminiClient geminiClient,
            ResumeTextExtractor resumeTextExtractor) {
        this.promptVersion = promptVersion;
        this.maxResumeChars = maxResumeChars;
        this.geminiClient = geminiClient;
        this.resumeTextExtractor = resumeTextExtractor;
    }

    public AiProgressDocument build(
            AiJobRequestedMessage message,
            ResumeVersion currentVersion,
            ResumeVersion previousVersion,
            AiFeedbackDocument previousFeedback
    ) {
        String currentResumeText = resumeTextExtractor.extract(currentVersion).orElse("");
        String previousResumeText = resumeTextExtractor.extract(previousVersion).orElse("");

        GeminiClient.GeminiProgressCallResult result = geminiClient.generateProgressAnalysisWithDiagnostics(
                buildPrompt(message, currentVersion, previousVersion, currentResumeText, previousResumeText, previousFeedback));
        GeminiClient.GeminiProgressAnalysis progress = result.analysis().orElse(null);
        if (progress == null) {
            throw new AiJobDomainException(
                    result.errorCode() == null ? "AI_PROVIDER_EMPTY_PROGRESS_RESPONSE" : result.errorCode(),
                    "Gemini progress failure. jobId=%s resumeVersionId=%s baselineResumeVersionId=%s detail=%s"
                            .formatted(message.jobId(), currentVersion.getId(), previousVersion.getId(), result.errorDetail()));
        }

        AiProgressDocument doc = new AiProgressDocument();
        doc.setJobId(message.jobId());
        doc.setResumeId(message.resumeId());
        doc.setResumeVersionId(currentVersion.getId());
        doc.setBaselineResumeVersionId(previousVersion.getId());
        doc.setOwnerId(message.ownerId());
        doc.setCreatedAt(Instant.now());
        doc.setModel(geminiClient.effectiveModel());
        doc.setPromptVersion(promptVersion);
        doc.setSummary(progress.summary());
        doc.setProgressStatus(progress.progressStatus());
        doc.setProgressScore(progress.progressScore());
        doc.setImprovedAreas(progress.improvedAreas());
        doc.setUnchangedIssues(progress.unchangedIssues());
        doc.setNewIssues(progress.newIssues());
        return doc;
    }

    private String buildPrompt(
            AiJobRequestedMessage message,
            ResumeVersion currentVersion,
            ResumeVersion previousVersion,
            String currentResumeText,
            String previousResumeText,
            AiFeedbackDocument previousFeedback
    ) {
        String previousFeedbackSection = previousFeedback == null ? "Feedback anterior: NAO DISPONIVEL" : """
                Feedback anterior dado ao utilizador:
                - summary: %s
                - strengths: %s
                - improvements: %s
                """.formatted(
                sanitize(previousFeedback.getSummary()),
                sanitizeList(previousFeedback.getStrengths()),
                sanitizeList(previousFeedback.getImprovements()));

        return """
                Voce e um revisor especializado em curriculos.
                Compare a nova versao com a versao anterior e com o feedback anterior.
                Escreva em portugues de Portugal, objetivo, sem frases genericas e sem markdown.
                RETORNE O JSON EM UMA UNICA LINHA.
                Responda SOMENTE com JSON valido no formato:
                {
                  "summary": "resumo curto sobre a progressao",
                  "progressStatus": "MELHOROU|ESTAVEL|REGREDIU",
                  "progressScore": 0,
                  "improvedAreas": ["ate 3 melhorias concretas"],
                  "unchangedIssues": ["ate 3 problemas que continuam"],
                  "newIssues": ["ate 3 novos problemas introduzidos"]
                }
                Regras:
                - Baseie-se no curriculo anterior, no curriculo atual e no feedback anterior.
                - progressScore deve ser inteiro entre 0 e 100.
                - Se nao houver evidencia suficiente, use listas vazias em vez de inventar.
                - Nao use aspas duplas dentro dos valores de texto.
                - Nao inclua quebra de linha nos valores.
                - Escape corretamente quaisquer caracteres especiais.

                Metadados:
                - jobId: %s
                - resumeId: %s
                - currentResumeVersionId: %s
                - baselineResumeVersionId: %s
                - ownerId: %s
                - limite de caracteres analisados: %s

                Curriculo anterior (texto bruto):
                %s

                %s

                Curriculo atual (texto bruto):
                %s
                """.formatted(
                message.jobId(),
                message.resumeId(),
                currentVersion.getId(),
                previousVersion.getId(),
                message.ownerId(),
                maxResumeChars,
                sanitize(previousResumeText),
                previousFeedbackSection,
                sanitize(currentResumeText));
    }

    private String sanitize(String value) {
        return value == null || value.isBlank() ? "NAO DISPONIVEL" : value;
    }

    private String sanitizeList(List<String> values) {
        return values == null || values.isEmpty() ? "[]" : values.toString();
    }
}
