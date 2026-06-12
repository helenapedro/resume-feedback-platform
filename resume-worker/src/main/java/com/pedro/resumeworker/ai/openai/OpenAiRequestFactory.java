package com.pedro.resumeworker.ai.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;

@Component
class OpenAiRequestFactory {

    private static final String FEEDBACK_SCHEMA_NAME = "ai_feedback";
    private static final String PROGRESS_SCHEMA_NAME = "ai_progress";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String JSON_SCHEMA_FORMAT = "json_schema";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(45);

    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;
    private final OpenAiSchemaFactory schemaFactory;

    OpenAiRequestFactory(
            OpenAiProperties properties,
            ObjectMapper objectMapper,
            OpenAiSchemaFactory schemaFactory) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.schemaFactory = schemaFactory;
    }

    HttpRequest feedbackRequest(String prompt) throws IOException {
        return buildRequest(prompt, FEEDBACK_SCHEMA_NAME, schemaFactory.feedbackSchema());
    }

    HttpRequest progressRequest(String prompt) throws IOException {
        return buildRequest(prompt, PROGRESS_SCHEMA_NAME, schemaFactory.progressSchema());
    }

    private HttpRequest buildRequest(String prompt, String schemaName, ObjectNode schema) throws IOException {
        return HttpRequest.newBuilder()
                .uri(buildUri())
                .timeout(REQUEST_TIMEOUT)
                .header(AUTHORIZATION_HEADER, "Bearer " + properties.apiKey())
                .header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .POST(HttpRequest.BodyPublishers.ofString(buildPayload(prompt, schemaName, schema)))
                .build();
    }

    private String buildPayload(String prompt, String schemaName, ObjectNode schema) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.effectiveModel());
        root.put("input", prompt);
        root.put("temperature", properties.temperature());
        root.put("max_output_tokens", properties.maxOutputTokens());
        root.put("store", false);
        root.set("text", textFormat(schemaName, schema));

        return objectMapper.writeValueAsString(root);
    }

    private ObjectNode textFormat(String schemaName, ObjectNode schema) {
        ObjectNode text = objectMapper.createObjectNode();
        ObjectNode format = objectMapper.createObjectNode();
        format.put("type", JSON_SCHEMA_FORMAT);
        format.put("name", schemaName);
        format.put("strict", true);
        format.set("schema", schema);
        text.set("format", format);
        return text;
    }

    private URI buildUri() {
        return URI.create(properties.effectiveBaseUrl() + "/v1/responses");
    }
}
