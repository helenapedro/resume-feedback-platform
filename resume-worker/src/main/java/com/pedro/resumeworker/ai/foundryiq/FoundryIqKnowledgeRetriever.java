package com.pedro.resumeworker.ai.foundryiq;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
class FoundryIqKnowledgeRetriever {

    private static final String AZURE_SEARCH_SOURCE = "azure-search";

    private final FoundryIqProperties properties;
    private final AzureSearchFoundryIqClient azureSearchClient;
    private final LocalFoundryIqKnowledgeSource localKnowledgeSource;

    FoundryIqKnowledgeRetriever(
            FoundryIqProperties properties,
            AzureSearchFoundryIqClient azureSearchClient,
            LocalFoundryIqKnowledgeSource localKnowledgeSource) {
        this.properties = properties;
        this.azureSearchClient = azureSearchClient;
        this.localKnowledgeSource = localKnowledgeSource;
    }

    List<FoundryIqKnowledgeSource> retrieve(String query) {
        if (AZURE_SEARCH_SOURCE.equalsIgnoreCase(properties.source())) {
            List<FoundryIqKnowledgeSource> searchResults = azureSearchClient.search(query);
            if (!searchResults.isEmpty()) {
                return searchResults;
            }
        }
        return localKnowledgeSource.retrieve();
    }
}
