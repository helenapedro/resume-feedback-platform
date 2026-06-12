package com.pedro.resumeworker.ai.azureopenai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.ai-feedback.azure-openai")
public record AzureOpenAiProperties(
        boolean enabled,
        String apiKey,
        String endpoint,
        String deployment,
        String apiVersion,
        double temperature,
        int maxOutputTokens
) {
    static final String DEFAULT_API_VERSION = "2024-08-01-preview";
    private static final String UNSET_DEPLOYMENT = "unset-deployment";

    boolean hasRequiredProviderConfig() {
        return enabled()
                && StringUtils.hasText(apiKey())
                && StringUtils.hasText(endpoint())
                && StringUtils.hasText(deployment());
    }

    String effectiveApiVersion() {
        return StringUtils.hasText(apiVersion()) ? apiVersion() : DEFAULT_API_VERSION;
    }

    String effectiveDeployment() {
        return StringUtils.hasText(deployment()) ? deployment() : UNSET_DEPLOYMENT;
    }
}
