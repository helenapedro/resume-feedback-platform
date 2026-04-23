package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.common.ai.Language;
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

    private final String promptVersion;
    private final int maxResumeChars;
    private final GeminiClient geminiClient;
    private final ResumeTextExtractor resumeTextExtractor;

    public AiFeedbackFactory(
            @Value("${app.ai-feedback.prompt-version:v1}") String promptVersion,
            @Value("${app.ai-feedback.max-resume-chars:12000}") int maxResumeChars,
            GeminiClient geminiClient,
            ResumeTextExtractor resumeTextExtractor) {
        this.promptVersion = promptVersion;
        this.maxResumeChars = maxResumeChars;
        this.geminiClient = geminiClient;
        this.resumeTextExtractor = resumeTextExtractor;
    }

    public AiFeedbackDocument build(AiJobRequestedMessage message, ResumeVersion resumeVersion) {
        String resumeText = resumeTextExtractor.extract(resumeVersion).orElse("");
        Language language = message.language() == null ? Language.EN : message.language();

        GeminiClient.GeminiCallResult result = geminiClient
                .generateFeedbackWithDiagnostics(buildPrompt(message, resumeText, language));
        GeminiClient.GeminiFeedback feedback = result.feedback().orElse(null);
        if (feedback == null) {
            throw new AiJobDomainException(
                    result.errorCode() == null ? "AI_PROVIDER_EMPTY_RESPONSE" : result.errorCode(),
                    "Gemini feedback failure. jobId=%s resumeVersionId=%s extractedText=%s detail=%s"
                            .formatted(
                                    message.jobId(),
                                    message.resumeVersionId(),
                                    !resumeText.isBlank(),
                                    result.errorDetail()));
        }

        AiFeedbackDocument doc = new AiFeedbackDocument();
        doc.setJobId(message.jobId());
        doc.setResumeId(message.resumeId());
        doc.setResumeVersionId(message.resumeVersionId());
        doc.setOwnerId(message.ownerId());
        doc.setCreatedAt(Instant.now());
        doc.setModel(geminiClient.effectiveModel());
        doc.setPromptVersion(promptVersion);
        doc.setSummary(feedback.summary());
        doc.setStrengths(feedback.strengths());
        doc.setImprovements(feedback.improvements());
        return doc;
    }

    private String buildPrompt(AiJobRequestedMessage message, String resumeText, Language language) {
        String contentSection;
        if (resumeText == null || resumeText.isBlank()) {
            contentSection = language == Language.PT ? "Curriculo extraido: NAO DISPONIVEL"
                    : "Resume extracted: NOT AVAILABLE";
        } else {
            contentSection = language == Language.PT ? """
                    Curriculo extraido (texto bruto):
                    %s
                    """.formatted(resumeText) : """
                    Resume extracted (raw text):
                    %s
                    """.formatted(resumeText);
        }

        if (language == Language.PT) {
            return """
                    Voce e um revisor especializado em curriculos.
                    Escreva em portugues de Portugal, objetivo, sem frases genericas e sem repetir sempre os mesmos pontos.
                    Nao use markdown. Nao use bloco ```json```.
                    RETORNE O JSON EM UMA UNICA LINHA.
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
                    - Nao use aspas duplas dentro dos valores de texto.
                    - Nao inclua quebra de linha nos valores.
                    - Escape corretamente quaisquer caracteres especiais.

                    Metadados:
                    - jobId: %s
                    - resumeId: %s
                    - resumeVersionId: %s
                    - ownerId: %s
                    - limite de caracteres analisados: %s

                    %s
                    """
                    .formatted(
                            message.jobId(),
                            message.resumeId(),
                            message.resumeVersionId(),
                            message.ownerId(),
                            maxResumeChars,
                            contentSection);
        }

        return """
                You are a resume reviewer.
                Write in clear English, objective, without generic phrases, and avoid repeating the same points.
                Do not use markdown. Do not use a ```json``` block.
                RETURN THE JSON IN A SINGLE LINE.
                Respond ONLY with valid JSON in the format:
                {
                  "summary": "one-sentence summary",
                  "strengths": ["3 specific strengths", "...", "..."],
                  "improvements": ["3 actionable improvements", "...", "..."]
                }
                Rules:
                - Each item should be concrete and different from the others.
                - Avoid clichés like "clear structure" or "add metrics" if there is no evidence.
                - When there is little information, explain that in the summary and still propose practical improvements.
                - Do not use double quotes inside text values.
                - Do not include line breaks in values.
                - Escape any special characters correctly.

                Metadata:
                - jobId: %s
                - resumeId: %s
                - resumeVersionId: %s
                - ownerId: %s
                - analyzed character limit: %s

                %s
                """
                .formatted(
                        message.jobId(),
                        message.resumeId(),
                        message.resumeVersionId(),
                        message.ownerId(),
                        maxResumeChars,
                        contentSection);
    }
}
