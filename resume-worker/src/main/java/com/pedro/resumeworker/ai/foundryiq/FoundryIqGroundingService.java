package com.pedro.resumeworker.ai.foundryiq;

import com.pedro.common.ai.Language;
import com.pedro.common.ai.mongo.AiFeedbackDocument;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
class FoundryIqGroundingService implements FoundryIqGroundingProvider {

    private static final String FEEDBACK_QUERY_EN =
            "resume review rubric software engineering recruiter impact metrics ownership technical depth";
    private static final String FEEDBACK_QUERY_PT =
            "rubrica de avaliacao de curriculo engenharia software impacto metricas ownership profundidade tecnica";
    private static final String PROGRESS_QUERY_EN =
            "resume version comparison progress analysis previous feedback improved unchanged new issues";
    private static final String PROGRESS_QUERY_PT =
            "comparacao de versoes de curriculo progresso feedback anterior melhorias problemas inalterados";

    private final FoundryIqProperties properties;
    private final FoundryIqKnowledgeRetriever retriever;

    FoundryIqGroundingService(FoundryIqProperties properties, FoundryIqKnowledgeRetriever retriever) {
        this.properties = properties;
        this.retriever = retriever;
    }

    @Override
    public String feedbackGrounding(Language language, String resumeText) {
        return groundingSection(queryForFeedback(language, resumeText), language);
    }

    @Override
    public String progressGrounding(
            Language language,
            String currentResumeText,
            String previousResumeText,
            AiFeedbackDocument previousFeedback) {
        return groundingSection(queryForProgress(language, currentResumeText, previousResumeText, previousFeedback), language);
    }

    private String groundingSection(String query, Language language) {
        if (!properties.enabled()) {
            return "";
        }

        List<FoundryIqKnowledgeSource> sources = retriever.retrieve(query);
        if (sources.isEmpty()) {
            return "";
        }

        String heading = language == Language.PT
                ? "Contexto Microsoft IQ / Foundry IQ para grounding:"
                : "Microsoft IQ / Foundry IQ grounding context:";
        String instruction = language == Language.PT
                ? "Use este contexto apenas como rubrica citada. Nao invente evidencia que nao esteja no curriculo."
                : "Use this context only as a cited rubric. Do not invent resume evidence that is not in the resume.";

        StringBuilder section = new StringBuilder()
                .append(heading)
                .append("\n")
                .append(instruction)
                .append("\n");
        for (FoundryIqKnowledgeSource source : sources) {
            section.append("- [")
                    .append(source.title())
                    .append("] ")
                    .append(source.content())
                    .append(source.url().isBlank() ? "" : " Source: " + source.url())
                    .append("\n");
        }
        return truncate(section.toString().trim());
    }

    private String queryForFeedback(Language language, String resumeText) {
        String base = language == Language.PT ? FEEDBACK_QUERY_PT : FEEDBACK_QUERY_EN;
        return appendExcerpt(base, resumeText);
    }

    private String queryForProgress(
            Language language,
            String currentResumeText,
            String previousResumeText,
            AiFeedbackDocument previousFeedback) {
        String base = language == Language.PT ? PROGRESS_QUERY_PT : PROGRESS_QUERY_EN;
        String previousSummary = previousFeedback == null ? "" : previousFeedback.getSummary();
        return appendExcerpt(base, currentResumeText + "\n" + previousResumeText + "\n" + previousSummary);
    }

    private String appendExcerpt(String query, String text) {
        if (!StringUtils.hasText(text)) {
            return query;
        }
        String trimmed = text.trim();
        int maxChars = Math.min(800, trimmed.length());
        return query + "\n" + trimmed.substring(0, maxChars);
    }

    private String truncate(String section) {
        if (section.length() <= properties.maxContextChars()) {
            return section;
        }
        return section.substring(0, properties.maxContextChars()).trim()
                + "\n[TRUNCATED: Microsoft IQ grounding context exceeded configured limit]";
    }
}
