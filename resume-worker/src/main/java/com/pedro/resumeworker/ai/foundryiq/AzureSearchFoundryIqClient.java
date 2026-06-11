package com.pedro.resumeworker.ai.foundryiq;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
class AzureSearchFoundryIqClient {

    private static final String API_KEY_HEADER = "api-key";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_LOG_BODY_CHARS = 1000;

    private final FoundryIqProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    AzureSearchFoundryIqClient(
            FoundryIqProperties properties,
            ObjectMapper objectMapper,
            HttpClient foundryIqHttpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = foundryIqHttpClient;
    }

    List<FoundryIqKnowledgeSource> search(String query) {
        FoundryIqProperties.AzureSearch search = properties.azureSearch();
        if (!search.hasRequiredConfig()) {
            log.info("Foundry IQ Azure AI Search grounding requested but endpoint, index, or API key is missing");
            return List.of();
        }

        try {
            HttpResponse<String> response = httpClient.send(
                    request(query, search),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Foundry IQ Azure AI Search error status={} body={}",
                        response.statusCode(),
                        truncateForLog(response.body()));
                return List.of();
            }
            return parse(response.body(), search);
        } catch (IOException ex) {
            log.warn("Foundry IQ Azure AI Search response parsing failed: {}", ex.getMessage());
            return List.of();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Foundry IQ Azure AI Search request interrupted: {}", ex.getMessage());
            return List.of();
        }
    }

    private HttpRequest request(String query, FoundryIqProperties.AzureSearch search) throws IOException {
        return HttpRequest.newBuilder()
                .uri(buildUri(search))
                .timeout(REQUEST_TIMEOUT)
                .header(API_KEY_HEADER, search.apiKey())
                .header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .POST(HttpRequest.BodyPublishers.ofString(body(query, search)))
                .build();
    }

    private URI buildUri(FoundryIqProperties.AzureSearch search) {
        String endpoint = search.endpoint().endsWith("/")
                ? search.endpoint().substring(0, search.endpoint().length() - 1)
                : search.endpoint();
        String indexName = URLEncoder.encode(search.indexName(), StandardCharsets.UTF_8);
        return URI.create("%s/indexes('%s')/docs/search.post.search?api-version=%s"
                .formatted(endpoint, indexName, search.apiVersion()));
    }

    private String body(String query, FoundryIqProperties.AzureSearch search) throws IOException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("search", query);
        body.put("queryType", search.queryType());
        body.put("top", search.top());
        body.put("select", String.join(",", search.titleField(), search.contentField(), search.urlField()));
        body.put("captions", "extractive|highlight-false");
        body.put("answers", "extractive|count-3");
        if (StringUtils.hasText(search.semanticConfiguration())) {
            body.put("semanticConfiguration", search.semanticConfiguration());
            body.put("semanticErrorHandling", "partial");
        }
        return objectMapper.writeValueAsString(body);
    }

    private List<FoundryIqKnowledgeSource> parse(String body, FoundryIqProperties.AzureSearch search)
            throws IOException {
        AzureSearchResponse response = objectMapper.readValue(body, AzureSearchResponse.class);
        List<FoundryIqKnowledgeSource> sources = new ArrayList<>();
        for (AzureSearchResult result : response.value()) {
            String content = firstCaption(result).orElse(result.field(search.contentField()));
            if (StringUtils.hasText(content)) {
                sources.add(new FoundryIqKnowledgeSource(
                        fallback(result.field(search.titleField()), "Azure AI Search result"),
                        content.trim(),
                        fallback(result.field(search.urlField()), "")));
            }
        }
        return sources;
    }

    private java.util.Optional<String> firstCaption(AzureSearchResult result) {
        if (result.captions() == null || result.captions().isEmpty()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.ofNullable(result.captions().get(0).text())
                .filter(StringUtils::hasText);
    }

    private String fallback(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String truncateForLog(String value) {
        if (value == null || value.length() <= MAX_LOG_BODY_CHARS) {
            return value;
        }
        return value.substring(0, MAX_LOG_BODY_CHARS) + "...";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AzureSearchResponse(List<AzureSearchResult> value) {
        AzureSearchResponse {
            value = value == null ? List.of() : value;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class AzureSearchResult {
        private final Map<String, String> fields = new java.util.HashMap<>();

        @JsonProperty("@search.captions")
        private List<AzureSearchCaption> captions;

        @JsonAnySetter
        void setField(String key, Object value) {
            if (value != null) {
                fields.put(key, String.valueOf(value));
            }
        }

        String field(String name) {
            return fields.getOrDefault(name, "");
        }

        List<AzureSearchCaption> captions() {
            return captions == null ? List.of() : captions;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AzureSearchCaption(String text) {
    }
}
