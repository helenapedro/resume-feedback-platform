package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.common.ai.mongo.AiFeedbackDocument;
import com.pedro.resumeworker.ai.gemini.GeminiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@Slf4j
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
            log.warn("Using fallback AI feedback template for jobId={} resumeVersionId={}",
                    message.jobId(), message.resumeVersionId());
            doc.setSummary("Feedback gerado automaticamente para o curriculo enviado.");
            doc.setStrengths(List.of(
                    "Estrutura clara e objetiva.",
                    "Historico profissional com evolucao temporal."
            ));
            doc.setImprovements(List.of(
                    "Adicionar metricas de impacto nas experiencias.",
                    "Revisar palavras-chave especificas da vaga."
            ));
        }
        return doc;
    }

    private String buildPrompt(AiJobRequestedMessage message) {
        return """
                Voce e um revisor especializado em curriculos para mercado tech.
                Escreva em portugues do Brasil, objetivo, sem frases genericas e sem repetir sempre os mesmos pontos.
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
                """.formatted(
                message.jobId(),
                message.resumeId(),
                message.resumeVersionId(),
                message.ownerId()
        );
    }
}
