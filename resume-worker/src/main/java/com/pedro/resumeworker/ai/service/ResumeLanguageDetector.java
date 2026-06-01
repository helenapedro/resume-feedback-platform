package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.Language;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

@Component
public class ResumeLanguageDetector {

    private static final Set<String> PORTUGUESE_WORDS = Set.of(
            "experiencia", "formacao", "educacao", "competencias", "habilidades",
            "curriculo", "profissional", "trabalho", "projetos", "licenciatura",
            "mestrado", "universidade", "empresa", "gestao", "desenvolvimento");

    private static final Set<String> ENGLISH_WORDS = Set.of(
            "experience", "education", "skills", "summary", "professional",
            "work", "projects", "degree", "university", "company", "management",
            "development", "employment", "certifications", "achievements");

    public Language resolve(Language requestedLanguage, String resumeText) {
        if (requestedLanguage == Language.EN || requestedLanguage == Language.PT) {
            return requestedLanguage;
        }
        return detect(resumeText);
    }

    public Language detect(String resumeText) {
        if (resumeText == null || resumeText.isBlank()) {
            return Language.EN;
        }

        String normalized = normalize(resumeText);
        int portugueseScore = score(normalized, PORTUGUESE_WORDS);
        int englishScore = score(normalized, ENGLISH_WORDS);

        if (normalized.matches(".*\\b(ao|oes|cao|para|com|dos|das|uma|como|mais|pela|pelo)\\b.*")) {
            portugueseScore++;
        }
        if (normalized.matches(".*\\b(the|and|with|for|from|that|this|built|led)\\b.*")) {
            englishScore++;
        }

        return portugueseScore > englishScore ? Language.PT : Language.EN;
    }

    private int score(String normalized, Set<String> words) {
        int score = 0;
        for (String word : words) {
            if (normalized.contains(" " + word + " ")) {
                score++;
            }
        }
        return score;
    }

    private String normalize(String value) {
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return " " + withoutAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ") + " ";
    }
}
