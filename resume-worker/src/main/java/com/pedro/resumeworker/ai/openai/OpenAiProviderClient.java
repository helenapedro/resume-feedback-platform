package com.pedro.resumeworker.ai.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pedro.resumeworker.ai.provider.AiFeedbackResult;
import com.pedro.resumeworker.ai.provider.AiProgressResult;
import com.pedro.resumeworker.ai.provider.AiProviderClient;
import com.pedro.resumeworker.ai.provider.AiProviderFeedbackCallResult;
import com.pedro.resumeworker.ai.provider.AiProviderProgressCallResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class OpenAiProviderClient implements AiProviderClient {

    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiProviderClient(OpenAiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        log.info("OpenAI provider configured model={} maxOutputTokens={} temperature={}",
                effectiveModel(),
                properties.maxOutputTokens(),
                properties.temperature());
    }

    @Override
    public AiProviderFeedbackCallResult generateFeedback(String prompt) {
        if (!isEnabled()) {
            return AiProviderFeedbackCallResult.failure("AI_PROVIDER_DISABLED", "OpenAI is disabled or API key is missing");
        }

        OpenAiCallResult result = request(prompt, "ai_feedback", buildFeedbackSchema());
        if (result.text().isEmpty()) {
            return AiProviderFeedbackCallResult.failure(result.errorCode(), result.errorDetail());
        }

        try {
            OpenAiFeedbackPayload payload = objectMapper.readValue(result.text().orElseThrow(), OpenAiFeedbackPayload.class);
            return AiProviderFeedbackCallResult.success(new AiFeedbackResult(
                    normalize(payload.summary()),
                    sanitizeList(payload.strengths()),
                    sanitizeList(payload.improvements()),
                    providerModel()));
        } catch (IOException ex) {
            return AiProviderFeedbackCallResult.failure("AI_PROVIDER_INVALID_JSON", ex.getMessage());
        }
    }

    @Override
    public AiProviderProgressCallResult generateProgressAnalysis(String prompt) {
        if (!isEnabled()) {
            return AiProviderProgressCallResult.failure("AI_PROVIDER_DISABLED", "OpenAI is disabled or API key is missing");
        }

        OpenAiCallResult result = request(prompt, "ai_progress", buildProgressSchema());
        if (result.text().isEmpty()) {
            return AiProviderProgressCallResult.failure(result.errorCode(), result.errorDetail());
        }

        try {
            OpenAiProgressPayload payload = objectMapper.readValue(result.text().orElseThrow(), OpenAiProgressPayload.class);
            return AiProviderProgressCallResult.success(new AiProgressResult(
                    normalize(payload.summary()),
                    normalize(payload.progressStatus()),
                    payload.progressScore(),
                    sanitizeList(payload.improvedAreas()),
                    sanitizeList(payload.unchangedIssues()),
                    sanitizeList(payload.newIssues()),
                    providerModel()));
        } catch (IOException ex) {
            return AiProviderProgressCallResult.failure("AI_PROVIDER_INVALID_JSON", ex.getMessage());
        }
    }

    @Override
    public String providerName() {
        return "openai";
    }

    @Override
    public String effectiveModel() {
        return StringUtils.hasText(properties.model())
                ? properties.model()
                : "gpt-4o-mini";
    }

    private boolean isEnabled() {
        return properties.enabled() && StringUtils.hasText(properties.apiKey());
    }

    private OpenAiCallResult request(String prompt, String schemaName, ObjectNode schema) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(buildUri())
                    .timeout(Duration.ofSeconds(45))
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(buildPayload(prompt, schemaName, schema)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("OpenAI HTTP error status={} body={}", response.statusCode(), truncateForLog(response.body()));
                return OpenAiCallResult.failure("AI_PROVIDER_HTTP_ERROR", "status=%s body=%s"
                        .formatted(response.statusCode(), truncateForLog(response.body())));
            }

            OpenAiResponse openAiResponse = objectMapper.readValue(response.body(), OpenAiResponse.class);
            if (openAiResponse.error() != null) {
                return OpenAiCallResult.failure("AI_PROVIDER_ERROR", openAiResponse.error().message());
            }
            if (!"completed".equalsIgnoreCase(openAiResponse.status())) {
                String reason = openAiResponse.incompleteDetails() == null
                        ? openAiResponse.status()
                        : openAiResponse.incompleteDetails().reason();
                String code = "max_output_tokens".equalsIgnoreCase(reason)
                        ? "AI_PROVIDER_MAX_TOKENS"
                        : "AI_PROVIDER_INCOMPLETE";
                return OpenAiCallResult.failure(code, "status=%s reason=%s".formatted(openAiResponse.status(), reason));
            }
            Optional<String> refusal = openAiResponse.firstRefusal();
            if (refusal.isPresent()) {
                return OpenAiCallResult.failure("AI_PROVIDER_BLOCKED", refusal.get());
            }
            return openAiResponse.firstOutputText()
                    .map(OpenAiCallResult::success)
                    .orElseGet(() -> OpenAiCallResult.failure("AI_PROVIDER_EMPTY_RESPONSE", "No output_text in OpenAI response"));
        } catch (IOException ex) {
            return OpenAiCallResult.failure("AI_PROVIDER_INVALID_RESPONSE", ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return OpenAiCallResult.failure("AI_PROVIDER_INTERRUPTED", ex.getMessage());
        }
    }

    private String buildPayload(String prompt, String schemaName, ObjectNode schema) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", effectiveModel());
        root.put("input", prompt);
        root.put("temperature", properties.temperature());
        root.put("max_output_tokens", properties.maxOutputTokens());
        root.put("store", false);

        ObjectNode text = objectMapper.createObjectNode();
        ObjectNode format = objectMapper.createObjectNode();
        format.put("type", "json_schema");
        format.put("name", schemaName);
        format.put("strict", true);
        format.set("schema", schema);
        text.set("format", format);
        root.set("text", text);

        return objectMapper.writeValueAsString(root);
    }

    private ObjectNode buildFeedbackSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.set("additionalProperties", objectMapper.getNodeFactory().booleanNode(false));

        ObjectNode propertiesNode = objectMapper.createObjectNode();
        propertiesNode.set("summary", stringSchema());
        propertiesNode.set("strengths", stringArraySchema(3, 3));
        propertiesNode.set("improvements", stringArraySchema(3, 3));
        schema.set("properties", propertiesNode);

        ArrayNode required = objectMapper.createArrayNode();
        required.add("summary");
        required.add("strengths");
        required.add("improvements");
        schema.set("required", required);
        return schema;
    }

    private ObjectNode buildProgressSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.set("additionalProperties", objectMapper.getNodeFactory().booleanNode(false));

        ObjectNode propertiesNode = objectMapper.createObjectNode();
        propertiesNode.set("summary", stringSchema());
        propertiesNode.set("progressStatus", stringSchema());
        ObjectNode progressScore = objectMapper.createObjectNode();
        progressScore.put("type", "integer");
        propertiesNode.set("progressScore", progressScore);
        propertiesNode.set("improvedAreas", stringArraySchema(2, 5));
        propertiesNode.set("unchangedIssues", stringArraySchema(2, 5));
        propertiesNode.set("newIssues", stringArraySchema(0, 5));
        schema.set("properties", propertiesNode);

        ArrayNode required = objectMapper.createArrayNode();
        required.add("summary");
        required.add("progressStatus");
        required.add("progressScore");
        required.add("improvedAreas");
        required.add("unchangedIssues");
        required.add("newIssues");
        schema.set("required", required);
        return schema;
    }

    private ObjectNode stringSchema() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", "string");
        return node;
    }

    private ObjectNode stringArraySchema(int minItems, int maxItems) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", "array");
        node.put("minItems", minItems);
        node.put("maxItems", maxItems);
        node.set("items", stringSchema());
        return node;
    }

    private URI buildUri() {
        String baseUrl = StringUtils.hasText(properties.baseUrl())
                ? properties.baseUrl()
                : "https://api.openai.com";
        return URI.create(baseUrl + "/v1/responses");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private List<String> sanitizeList(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }

    private String truncateForLog(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000) + "...";
    }

    private record OpenAiCallResult(Optional<String> text, String errorCode, String errorDetail) {
        static OpenAiCallResult success(String text) {
            return new OpenAiCallResult(Optional.ofNullable(text), null, null);
        }

        static OpenAiCallResult failure(String errorCode, String errorDetail) {
            return new OpenAiCallResult(Optional.empty(), errorCode, errorDetail);
        }
    }

    private record OpenAiFeedbackPayload(String summary, List<String> strengths, List<String> improvements) {
    }

    private record OpenAiProgressPayload(
            String summary,
            String progressStatus,
            Integer progressScore,
            List<String> improvedAreas,
            List<String> unchangedIssues,
            List<String> newIssues
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiResponse(
            String status,
            OpenAiError error,
            @com.fasterxml.jackson.annotation.JsonProperty("incomplete_details") IncompleteDetails incompleteDetails,
            List<OutputItem> output
    ) {
        Optional<String> firstOutputText() {
            if (output == null) {
                return Optional.empty();
            }
            return output.stream()
                    .filter(item -> item.content() != null)
                    .flatMap(item -> item.content().stream())
                    .filter(content -> "output_text".equals(content.type()))
                    .map(ContentItem::text)
                    .filter(StringUtils::hasText)
                    .findFirst();
        }

        Optional<String> firstRefusal() {
            if (output == null) {
                return Optional.empty();
            }
            return output.stream()
                    .filter(item -> item.content() != null)
                    .flatMap(item -> item.content().stream())
                    .filter(content -> "refusal".equals(content.type()))
                    .map(ContentItem::refusal)
                    .filter(StringUtils::hasText)
                    .findFirst();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiError(String message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record IncompleteDetails(String reason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OutputItem(String type, List<ContentItem> content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ContentItem(String type, String text, String refusal) {
    }
}
