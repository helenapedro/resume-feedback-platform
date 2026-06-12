package com.pedro.resumeworker.ai.foundryiq;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.ai-feedback.foundry-iq")
public record FoundryIqProperties(
        boolean enabled,
        String source,
        int maxContextChars,
        AzureSearch azureSearch
) {
    private static final int DEFAULT_MAX_CONTEXT_CHARS = 1800;

    public int maxContextChars() {
        return maxContextChars > 0 ? maxContextChars : DEFAULT_MAX_CONTEXT_CHARS;
    }

    public String source() {
        return StringUtils.hasText(source) ? source : "local";
    }

    public AzureSearch azureSearch() {
        return azureSearch == null ? AzureSearch.empty() : azureSearch;
    }

    public record AzureSearch(
            String endpoint,
            String indexName,
            String apiKey,
            String apiVersion,
            String queryType,
            String semanticConfiguration,
            String contentField,
            String titleField,
            String urlField,
            int top
    ) {
        private static final String DEFAULT_API_VERSION = "2024-07-01";
        private static final String DEFAULT_QUERY_TYPE = "semantic";
        private static final String DEFAULT_CONTENT_FIELD = "content";
        private static final String DEFAULT_TITLE_FIELD = "title";
        private static final String DEFAULT_URL_FIELD = "url";
        private static final int DEFAULT_TOP = 3;

        static AzureSearch empty() {
            return new AzureSearch(null, null, null, null, null, null, null, null, null, 0);
        }

        boolean hasRequiredConfig() {
            return StringUtils.hasText(endpoint)
                    && StringUtils.hasText(indexName)
                    && StringUtils.hasText(apiKey);
        }

        public String apiVersion() {
            return StringUtils.hasText(apiVersion) ? apiVersion : DEFAULT_API_VERSION;
        }

        public String queryType() {
            return StringUtils.hasText(queryType) ? queryType : DEFAULT_QUERY_TYPE;
        }

        public String contentField() {
            return StringUtils.hasText(contentField) ? contentField : DEFAULT_CONTENT_FIELD;
        }

        public String titleField() {
            return StringUtils.hasText(titleField) ? titleField : DEFAULT_TITLE_FIELD;
        }

        public String urlField() {
            return StringUtils.hasText(urlField) ? urlField : DEFAULT_URL_FIELD;
        }

        public int top() {
            return top > 0 ? top : DEFAULT_TOP;
        }
    }
}
