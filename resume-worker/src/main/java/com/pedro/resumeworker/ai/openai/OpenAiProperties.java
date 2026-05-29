package com.pedro.resumeworker.ai.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai-feedback.openai")
public record OpenAiProperties(
        boolean enabled,
        String apiKey,
        String baseUrl,
        String model,
        double temperature,
        int maxOutputTokens
) {
}
