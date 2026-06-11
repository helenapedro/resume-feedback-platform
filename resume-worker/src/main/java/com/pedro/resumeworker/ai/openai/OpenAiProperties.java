package com.pedro.resumeworker.ai.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.ai-feedback.openai")
public record OpenAiProperties(
        boolean enabled,
        String apiKey,
        String baseUrl,
        String model,
        double temperature,
        int maxOutputTokens
) {
    static final String DEFAULT_BASE_URL = "https://api.openai.com";
    static final String DEFAULT_MODEL = "gpt-4o-mini";

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
