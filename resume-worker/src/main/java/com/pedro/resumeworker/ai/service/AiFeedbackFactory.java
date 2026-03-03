package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.common.ai.mongo.AiFeedbackDocument;
import com.pedro.resumeworker.ai.domain.ResumeVersion;
import com.pedro.resumeworker.ai.gemini.GeminiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
public class AiFeedbackFactory {

    private final String model;
    private final String promptVersion;
    private final int maxResumeChars;
    private final GeminiClient geminiClient;
    private final ResumeTextExtractor resumeTextExtractor;

    public AiFeedbackFactory(
            @Value("${app.ai-feedback.model:gpt-4o-mini}") String model,
            @Value("${app.ai-feedback.prompt-version:v1}") String promptVersion,
            @Value("${app.ai-feedback.max-resume-chars:12000}") int maxResumeChars,
            GeminiClient geminiClient,
            ResumeTextExtractor resumeTextExtractor) {
        this.model = model;
        this.promptVersion = promptVersion;
        this.maxResumeChars = maxResumeChars;
        this.geminiClient = geminiClient;
        this.resumeTextExtractor = resumeTextExtractor;
    }

    public AiFeedbackDocument build(AiJobRequestedMessage message, ResumeVersion resumeVersion) {
        String resumeText = resumeTextExtractor.extract(resumeVersion).orElse("");

        GeminiClient.GeminiFeedback feedback = geminiClient
                .generateFeedback(buildPrompt(message, resumeText))
                .orElse(null);
        if (feedback == null) {
            throw new IllegalStateException("GEMINI_FEEDBACK_INVALID_OR_EMPTY: jobId=%s resumeVersionId=%s extractedText=%s"
                    .formatted(message.jobId(), message.resumeVersionId(), !resumeText.isBlank()));
        }

        AiFeedbackDocument doc = new AiFeedbackDocument();
        doc.setJobId(message.jobId());
        doc.setResumeId(message.resumeId());
        doc.setResumeVersionId(message.resumeVersionId());
        doc.setOwnerId(message.ownerId());
        doc.setCreatedAt(Instant.now());
        doc.setModel(model);
        doc.setPromptVersion(promptVersion);
        doc.setSummary(feedback.summary());
        doc.setStrengths(feedback.strengths());
        doc.setImprovements(feedback.improvements());
        return doc;
    }

    private String buildPrompt(AiJobRequestedMessage message, String resumeText) {
        String contentSection;
        if (resumeText == null || resumeText.isBlank()) {
            contentSection = "Curriculo extraido: NAO DISPONIVEL";
        } else {
            contentSection = """
                    Curriculo extraido (texto bruto):
                    %s
                    """.formatted(resumeText);
        }

        return """
                Voce e um revisor especializado em curriculos.
                Escreva em portugues de Portugal, objetivo, sem frases genericas e sem repetir sempre os mesmos pontos.
                Nao use markdown. Nao use bloco ```json```.
                Responda SOMENTE com JSON valido no formato:
                {
                  "summary": "resumo em 1 frase",
                  "strengths": ["3 pontos fortes especificos", "...", "..."],
                  "improvements": ["3 melhorias acionaveis", "...", "..."]
                }
                Regras:
                - Cada item deve ser concreto e diferente dos demais.
                - Evite cliches como "estrutura clara" ou "adicionar metricas" se nao houver evidencia.
                - Quando houver pouca informacao, diga isso no summary e ainda proponha melhorias praticas.

                Metadados:
                - jobId: %s
                - resumeId: %s
                - resumeVersionId: %s
                - ownerId: %s
                - limite de caracteres analisados: %s

                %s
                """.formatted(
                message.jobId(),
                message.resumeId(),
                message.resumeVersionId(),
                message.ownerId(),
                maxResumeChars,
                contentSection);
    }
}
