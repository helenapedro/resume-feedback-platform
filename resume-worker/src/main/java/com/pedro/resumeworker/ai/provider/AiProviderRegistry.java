package com.pedro.resumeworker.ai.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class AiProviderRegistry {

    private final String configuredProvider;
    private final List<AiProviderClient> clients;

    public AiProviderRegistry(
            @Value("${app.ai.provider:gemini}") String configuredProvider,
            List<AiProviderClient> clients) {
        this.configuredProvider = configuredProvider;
        this.clients = List.copyOf(clients);
        log.info("AI provider registry configured provider={} availableProviders={}",
                configuredProvider,
                this.clients.stream().map(AiProviderClient::providerName).toList());
    }

    public AiProviderClient activeClient() {
        return clients.stream()
                .filter(client -> client.providerName().equalsIgnoreCase(configuredProvider))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No AI provider configured for '%s'. Available providers: %s"
                                .formatted(
                                        configuredProvider,
                                        clients.stream().map(AiProviderClient::providerName).toList())));
    }
}
