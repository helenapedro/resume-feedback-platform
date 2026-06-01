package com.pedro.resumeworker.ai.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ResumeDocumentClassifier {

    private static final int MAX_REASONABLE_RESUME_PAGES = 12;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\+?\\d[\\d .()/-]{7,}\\d)");
    private static final Set<String> RESUME_SIGNALS = Set.of(
            "experience", "work experience", "employment", "education", "skills", "projects",
            "certifications", "summary", "professional summary", "achievements", "linkedin",
            "experiencia", "experiencia profissional", "formacao", "educacao", "competencias",
            "habilidades", "projetos", "certificacoes", "perfil profissional", "resumo profissional");

    public boolean isLikelyResume(ResumeTextExtractor.ResumeTextExtraction extraction) {
        if (extraction == null || extraction.text() == null || extraction.text().isBlank()) {
            return false;
        }
        if (extraction.pageCount() > MAX_REASONABLE_RESUME_PAGES) {
            return false;
        }

        String normalized = normalize(extraction.text());
        int score = 0;
        for (String signal : RESUME_SIGNALS) {
            if (normalized.contains(" " + signal + " ")) {
                score++;
            }
        }
        if (EMAIL_PATTERN.matcher(extraction.text()).find()) {
            score++;
        }
        if (PHONE_PATTERN.matcher(extraction.text()).find()) {
            score++;
        }

        return score >= 2;
    }

    private String normalize(String value) {
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return " " + withoutAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ") + " ";
    }
}
