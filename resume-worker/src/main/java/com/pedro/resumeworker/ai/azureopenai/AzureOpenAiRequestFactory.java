package com.pedro.resumeworker.ai.azureopenai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
class AzureOpenAiRequestFactory {

    private static final String FEEDBACK_SCHEMA_NAME = "ai_feedback";
    private static final String PROGRESS_SCHEMA_NAME = "ai_progress";
    private static final String USER_ROLE = "user";
    private static final String JSON_SCHEMA_FORMAT = "json_schema";
    private static final String API_KEY_HEADER = "api-key";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(45);

    private final AzureOpenAiProperties properties;
    private final ObjectMapper objectMapper;
    private final AzureOpenAiSchemaFactory schemaFactory;

    AzureOpenAiRequestFactory(
            AzureOpenAiProperties properties,
            ObjectMapper objectMapper,
            AzureOpenAiSchemaFactory schemaFactory) {
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
                .header(API_KEY_HEADER, properties.apiKey())
                .header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .POST(HttpRequest.BodyPublishers.ofString(buildPayload(prompt, schemaName, schema)))
                .build();
    }

    private String buildPayload(String prompt, String schemaName, ObjectNode schema) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", USER_ROLE);
        userMessage.put("content", prompt);
        messages.add(userMessage);
        root.set("messages", messages);
        root.put("temperature", properties.temperature());
        root.put("max_tokens", properties.maxOutputTokens());
        root.set("response_format", responseFormat(schemaName, schema));

        return objectMapper.writeValueAsString(root);
    }

    private ObjectNode responseFormat(String schemaName, ObjectNode schema) {
        ObjectNode responseFormat = objectMapper.createObjectNode();
        responseFormat.put("type", JSON_SCHEMA_FORMAT);

        ObjectNode jsonSchema = objectMapper.createObjectNode();
        jsonSchema.put("name", schemaName);
        jsonSchema.put("strict", true);
        jsonSchema.set("schema", schema);
        responseFormat.set("json_schema", jsonSchema);
        return responseFormat;
    }

    private URI buildUri() {
        String endpoint = properties.endpoint().endsWith("/")
                ? properties.endpoint().substring(0, properties.endpoint().length() - 1)
                : properties.endpoint();
        String deployment = URLEncoder.encode(properties.deployment(), StandardCharsets.UTF_8);
        return URI.create("%s/openai/deployments/%s/chat/completions?api-version=%s"
                .formatted(endpoint, deployment, properties.effectiveApiVersion()));
    }
}
