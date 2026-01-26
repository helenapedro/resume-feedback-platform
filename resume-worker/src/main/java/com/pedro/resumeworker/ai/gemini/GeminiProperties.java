package com.pedro.resumeworker.ai.gemini;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai-feedback.gemini")
public record GeminiProperties(
        boolean enabled,
        String apiKey,
        String baseUrl,
        String model,
        double temperature,
        int maxOutputTokens
) {
}
