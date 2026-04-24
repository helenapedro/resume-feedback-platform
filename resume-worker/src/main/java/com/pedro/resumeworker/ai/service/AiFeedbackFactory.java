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
            @Value("${app.ai-feedback.prompt-version:v2}") String promptVersion,
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
                    Voce e um revisor senior de curriculos para vagas de engenharia de software em empresas exigentes.
                    Avalie como um recruiter tecnico ou hiring manager, com foco em clareza, profundidade tecnica, senioridade e impacto.
                    Escreva em portugues de Portugal, objetivo, direto, sem frases genericas e sem repetir sempre os mesmos pontos.
                    Nao use markdown. Nao use bloco ```json```.
                    RETORNE O JSON EM UMA UNICA LINHA.
                    Responda SOMENTE com JSON valido no formato:
                    {
                      "summary": "avaliacao geral em 2 ou 3 frases curtas",
                      "strengths": ["3 a 4 pontos fortes especificos", "...", "..."],
                      "improvements": ["4 a 5 melhorias acionaveis", "...", "..."]
                    }
                    Regras:
                    - O summary deve dizer quao competitivo o curriculo esta hoje e o que falta para chegar a um nivel mais forte.
                    - Cada item deve ser concreto, diferente dos demais e baseado em evidencia do curriculo.
                    - Priorize observacoes de alto valor sobre impacto e metricas, ownership, profundidade tecnica, sinais de system design, clareza e escaneabilidade, e nivel de senioridade.
                    - Evite cliches vagos como "boas capacidades", "estrutura clara", "adicionar metricas" ou "melhorar comunicacao" se nao houver evidencia.
                    - Nao invente experiencias, tecnologias, empresas, resultados ou numeros que nao estejam no curriculo.
                    - Se o curriculo ja for forte, diga isso claramente antes de apontar lacunas reais.
                    - As melhorias devem parecer conselho de carreira de alto nivel, nao critica superficial.
                    - Sempre que possivel, inclua a area alvo no inicio de cada item, por exemplo: "Experiencia:", "Projetos:", "Skills:", "Header:", "Senioridade:", "Clareza:".
                    - Quando fizer sentido, sugira como reescrever ou reposicionar o conteudo e explique o porque em linguagem curta.
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
                You are a senior resume reviewer for software engineering roles at demanding companies.
                Evaluate like a technical recruiter or hiring manager, with attention to clarity, technical depth, seniority signals, and real impact.
                Write in clear English, direct and objective, without generic phrases, and avoid repeating the same points.
                Do not use markdown. Do not use a ```json``` block.
                RETURN THE JSON IN A SINGLE LINE.
                Respond ONLY with valid JSON in the format:
                {
                  "summary": "overall assessment in 2-3 short sentences",
                  "strengths": ["3-4 specific strengths", "...", "..."],
                  "improvements": ["4-5 actionable improvements", "...", "..."]
                }
                Rules:
                - The summary should say how competitive the resume is now and what is still missing to reach a stronger tier.
                - Each item should be concrete, evidence-based, and different from the others.
                - Prioritize high-value observations about impact and metrics, ownership, technical depth, system design signals, clarity and scanability, and seniority level.
                - Avoid vague cliches like "strong communication", "clear structure", "add metrics", or "improve speaking skills" unless the resume clearly supports that point.
                - Do not invent experience, technologies, companies, outcomes, or numbers that are not present in the resume.
                - If the resume is already strong, say that clearly before calling out real gaps.
                - Improvements should sound like high-quality career coaching, not shallow criticism.
                - When possible, start each item with a target area such as "Experience:", "Projects:", "Skills:", "Header:", "Seniority:", or "Clarity:".
                - When useful, suggest how to rewrite or reposition the content and briefly explain why.
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
