package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.resumeworker.ai.mongo.AiFeedbackDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class AiFeedbackFactory {

    private final String model;
    private final String promptVersion;

    public AiFeedbackFactory(
            @Value("${app.ai-feedback.model:gpt-4o-mini}") String model,
            @Value("${app.ai-feedback.prompt-version:v1}") String promptVersion
    ) {
        this.model = model;
        this.promptVersion = promptVersion;
    }

    public AiFeedbackDocument build(AiJobRequestedMessage message) {
        AiFeedbackDocument doc = new AiFeedbackDocument();
        doc.setJobId(message.jobId());
        doc.setResumeId(message.resumeId());
        doc.setResumeVersionId(message.resumeVersionId());
        doc.setOwnerId(message.ownerId());
        doc.setCreatedAt(Instant.now());
        doc.setModel(model);
        doc.setPromptVersion(promptVersion);
        doc.setSummary("Feedback gerado automaticamente para o currículo enviado.");
        doc.setStrengths(List.of(
                "Estrutura clara e objetiva.",
                "Histórico profissional com evolução temporal."
        ));
        doc.setImprovements(List.of(
                "Adicionar métricas de impacto nas experiências.",
                "Revisar palavras-chave específicas da vaga."
        ));
        return doc;
    }
}
