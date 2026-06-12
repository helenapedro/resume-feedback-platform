package com.pedro.resumeworker.ai.gemini;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.ai-feedback.gemini")
public record GeminiProperties(
        boolean enabled,
        String apiKey,
        String baseUrl,
        String model,
        double temperature,
        int maxOutputTokens
) {
    static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com";
    static final String DEFAULT_MODEL = "gemini-1.5-flash";

    boolean hasRequiredProviderConfig() {
        return enabled() && StringUtils.hasText(apiKey());
    }

    String effectiveBaseUrl() {
        return StringUtils.hasText(baseUrl()) ? baseUrl() : DEFAULT_BASE_URL;
    }

    String effectiveModel() {
        return StringUtils.hasText(model()) ? model() : DEFAULT_MODEL;
    }
}
