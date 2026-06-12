package com.pedro.resumeworker.ai.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;

@Component
class GeminiRequestFactory {

    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;
    private final GeminiSchemaFactory schemaFactory;

    GeminiRequestFactory(
            GeminiProperties properties,
            ObjectMapper objectMapper,
            GeminiSchemaFactory schemaFactory) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.schemaFactory = schemaFactory;
    }

    HttpRequest feedbackRequest(String prompt, boolean useResponseSchema) throws IOException {
        return buildRequest(prompt, useResponseSchema, schemaFactory.feedbackSchema());
    }

    HttpRequest progressRequest(String prompt, boolean useResponseSchema) throws IOException {
        return buildRequest(prompt, useResponseSchema, schemaFactory.progressSchema());
    }

    private HttpRequest buildRequest(String prompt, boolean useResponseSchema, ObjectNode responseSchema)
            throws IOException {
        return HttpRequest.newBuilder()
                .uri(buildUri())
                .timeout(REQUEST_TIMEOUT)
                .header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .POST(HttpRequest.BodyPublishers.ofString(buildPayload(prompt, useResponseSchema, responseSchema)))
                .build();
    }

    private String buildPayload(String prompt, boolean useResponseSchema, ObjectNode responseSchema)
            throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode content = objectMapper.createObjectNode();
        content.put("role", "user");

        ObjectNode part = objectMapper.createObjectNode();
        part.put("text", prompt);
        content.set("parts", objectMapper.valueToTree(List.of(part)));
        root.set("contents", objectMapper.valueToTree(List.of(content)));

        ObjectNode config = objectMapper.createObjectNode();
        config.put("temperature", properties.temperature());
        config.put("maxOutputTokens", properties.maxOutputTokens());
        config.put("responseMimeType", "application/json");
        if (useResponseSchema) {
            config.set("responseSchema", responseSchema);
        }
        root.set("generationConfig", config);

        return objectMapper.writeValueAsString(root);
    }

    private URI buildUri() {
        return URI.create(String.format(
                "%s/v1beta/models/%s:generateContent?key=%s",
                properties.effectiveBaseUrl(),
                properties.effectiveModel(),
                properties.apiKey()
        ));
    }
}
