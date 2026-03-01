package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.common.ai.mongo.AiFeedbackDocument;
import com.pedro.resumeworker.ai.gemini.GeminiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class AiFeedbackFactory {

    private final String model;
    private final String promptVersion;
    private final GeminiClient geminiClient;

    public AiFeedbackFactory(
            @Value("${app.ai-feedback.model:gpt-4o-mini}") String model,
            @Value("${app.ai-feedback.prompt-version:v1}") String promptVersion,
            GeminiClient geminiClient
    ) {
        this.model = model;
        this.promptVersion = promptVersion;
        this.geminiClient = geminiClient;
    }

    public AiFeedbackDocument build(AiJobRequestedMessage message) {
        GeminiClient.GeminiFeedback feedback = geminiClient
                .generateFeedback(buildPrompt(message))
                .orElse(null);

        AiFeedbackDocument doc = new AiFeedbackDocument();
        doc.setJobId(message.jobId());
        doc.setResumeId(message.resumeId());
        doc.setResumeVersionId(message.resumeVersionId());
        doc.setOwnerId(message.ownerId());
        doc.setCreatedAt(Instant.now());
        doc.setModel(model);
        doc.setPromptVersion(promptVersion);
        if (feedback != null) {
            doc.setSummary(feedback.summary());
            doc.setStrengths(feedback.strengths());
            doc.setImprovements(feedback.improvements());
        } else {
            doc.setSummary("Feedback gerado automaticamente para o currículo enviado.");
            doc.setStrengths(List.of(
                    "Estrutura clara e objetiva.",
                    "Histórico profissional com evolução temporal."
            ));
            doc.setImprovements(List.of(
                    "Adicionar métricas de impacto nas experiências.",
                    "Revisar palavras-chave específicas da vaga."
            ));
        }
        return doc;
    }

    private String buildPrompt(AiJobRequestedMessage message) {
        return """
                Você é um revisor de currículos. Gere feedback conciso em português, com base no conteúdo disponível.
                Caso não haja conteúdo do currículo, responda com sugestões gerais.

                Responda SOMENTE com JSON válido no formato:
                {
                  "summary": "resumo curto",
                  "strengths": ["ponto forte 1", "ponto forte 2"],
                  "improvements": ["melhoria 1", "melhoria 2"]
                }

                Metadados:
                - jobId: %s
                - resumeId: %s
                - resumeVersionId: %s
                - ownerId: %s
                """.formatted(
                message.jobId(),
                message.resumeId(),
                message.resumeVersionId(),
                message.ownerId()
        );
    }
}
