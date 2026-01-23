package com.pedro.resumeworker.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.ai-jobs.retry")
public record AiJobRetryProperties(
        int maxAttempts,
        Duration initialBackoff,
        Duration maxBackoff
) {
}
