package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.common.ai.Language;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AiFeedbackPromptBuilder {

    private final int maxResumeChars;

    public AiFeedbackPromptBuilder(@Value("${app.ai-feedback.max-resume-chars:12000}") int maxResumeChars) {
        this.maxResumeChars = maxResumeChars;
    }

    public String build(AiJobRequestedMessage message, String resumeText, Language language) {
        String contentSection = buildContentSection(resumeText, language);

        if (language == Language.PT) {
            return """
                    Voce e um recruiter tecnico senior a fazer uma leitura exigente de curriculos para vagas de engenharia de software.
                    Avalie competitividade, senioridade, clareza, profundidade tecnica, ownership e impacto mensuravel.
                    Escreva em portugues de Portugal, de forma direta, concreta, sem frases genericas, sem elogios vazios e sem repetir a mesma ideia.
                    Nao use markdown. Nao use bloco ```json```.
                    RETORNE O JSON EM UMA UNICA LINHA.
                    Responda SOMENTE com JSON valido no formato:
                    {
                      "summary": "avaliacao executiva em 2 ou 3 frases curtas",
                      "strengths": ["3 pontos fortes especificos", "...", "..."],
                      "improvements": ["3 prioridades de maior alavancagem", "...", "..."]
                    }
                    Objetivo:
                    - O summary deve dizer em que nivel o curriculo se posiciona hoje, porque cria ou nao confianca, e quais lacunas mais afetam a decisao de entrevista.
                    - strengths deve listar sinais que ja ajudam a candidatura. Cada item deve ligar evidencia do curriculo ao motivo pelo qual isso aumenta confianca do recruiter.
                    - improvements deve listar as 3 correcoes com maior impacto. Cada item deve dizer o problema, o que mudar no curriculo e porque isso melhora fit, senioridade ou clareza.
                    Regras:
                    - Baseie-se apenas no curriculo. Nao invente empresas, cargos, tecnologias, resultados ou numeros.
                    - Cada item deve ser especifico e diferente. Proiba observacoes vagas como "boa comunicacao", "estrutura clara", "adicionar metricas" ou "melhorar clareza" sem contexto.
                    - Prefira comentarios sobre impacto e metricas, escopo e ownership, profundidade tecnica, decisoes de arquitetura, progressao de senioridade, posicionamento para o alvo e escaneabilidade.
                    - Se o curriculo ja for forte, diga isso com clareza, mas aponte ainda as lacunas reais que separam o candidato de um nivel mais forte.
                    - Se houver pouca informacao, diga isso explicitamente e transforme a falta de evidencia em melhorias praticas.
                    - Comece cada item com uma area alvo, por exemplo: "Experiencia:", "Projetos:", "Skills:", "Resumo:", "Senioridade:", "Clareza:".
                    - Sempre que fizer sentido, sugira reposicionamento ou reescrita concreta em linguagem curta.
                    - Cada item deve caber numa frase curta ou em duas frases curtas no maximo.
                    - Nao use aspas duplas dentro dos valores.
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
                You are a senior technical recruiter reviewing resumes for demanding software engineering roles.
                Judge competitiveness, seniority, clarity, technical depth, ownership, and measurable impact.
                Write in direct English, concrete and specific, with no filler, no empty praise, and no repeated points.
                Do not use markdown. Do not use a ```json``` block.
                RETURN THE JSON IN A SINGLE LINE.
                Respond ONLY with valid JSON in the format:
                {
                  "summary": "executive assessment in 2-3 short sentences",
                  "strengths": ["3 specific recruiter-positive signals", "...", "..."],
                  "improvements": ["3 highest-leverage fixes", "...", "..."]
                }
                Objective:
                - The summary must say what tier the resume reads at today, why it does or does not create recruiter confidence, and which gaps most affect interview likelihood.
                - strengths must list signals already helping the candidacy. Each item must connect resume evidence to why it increases recruiter confidence.
                - improvements must list the 3 highest-leverage fixes. Each item must state the problem, what to change in the resume, and why that change improves fit, seniority, or clarity.
                Rules:
                - Use only evidence from the resume. Do not invent companies, roles, technologies, outcomes, or numbers.
                - Every item must be specific and materially different. Ban vague comments like "strong communication", "clear structure", "add metrics", or "improve clarity" unless the resume itself justifies them with context.
                - Prefer observations about measurable impact, ownership and scope, technical depth, architecture or system design decisions, seniority progression, target-role positioning, and scanability.
                - If the resume is already strong, say that plainly, but still identify the real gaps that separate it from a stronger tier.
                - If there is limited evidence, say so explicitly and turn the missing proof into practical fixes.
                - Start every item with a target area such as "Experience:", "Projects:", "Skills:", "Summary:", "Seniority:", or "Clarity:".
                - When useful, suggest a concrete rewrite or repositioning in brief language.
                - Each item should fit in one short sentence or two short sentences at most.
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
