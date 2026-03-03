package com.pedro.resumeworker.ai.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.json.JsonReadFeature;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class GeminiClient {

    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;
    private final ObjectMapper lenientObjectMapper;
    private final HttpClient httpClient;

    public GeminiClient(GeminiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.lenientObjectMapper = JsonMapper.builder()
                .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
                .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .build();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public Optional<GeminiFeedback> generateFeedback(String prompt) {
        return generateFeedbackWithDiagnostics(prompt).feedback();
    }

    public GeminiCallResult generateFeedbackWithDiagnostics(String prompt) {
        if (!properties.enabled() || !StringUtils.hasText(properties.apiKey())) {
            return GeminiCallResult.failure("AI_PROVIDER_DISABLED", "Gemini is disabled or API key is missing");
        }

        GeminiCallResult strictAttempt = requestFeedback(prompt, true, true);
        if (strictAttempt.feedback().isPresent()) {
            return strictAttempt;
        }
        if ("AI_PROVIDER_BLOCKED".equals(strictAttempt.errorCode())) {
            return strictAttempt;
        }

        // Fallback attempt: remove schema constraints to recover from provider-side structured output issues.
        GeminiCallResult fallbackAttempt = requestFeedback(prompt, false, true);
        if (fallbackAttempt.feedback().isPresent()) {
            return fallbackAttempt;
        }
        if ("AI_PROVIDER_INVALID_JSON".equals(fallbackAttempt.errorCode())) {
            GeminiCallResult compactJsonAttempt = requestFeedback(
                    prompt + "\n\nIMPORTANT: Return strictly valid JSON in ONE LINE only. No markdown. No comments.",
                    false,
                    true);
            if (compactJsonAttempt.feedback().isPresent()) {
                return compactJsonAttempt;
            }
            return compactJsonAttempt.errorCode() != null ? compactJsonAttempt : fallbackAttempt;
        }
        return fallbackAttempt.errorCode() != null ? fallbackAttempt : strictAttempt;
    }

    public String effectiveModel() {
        return StringUtils.hasText(properties.model())
                ? properties.model()
                : "gemini-1.5-flash";
    }

    private GeminiCallResult requestFeedback(String prompt, boolean useResponseSchema, boolean allowRepair) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(buildUri())
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(buildPayload(prompt, useResponseSchema)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Gemini HTTP error (schema={}) status={} body={}",
                        useResponseSchema,
                        response.statusCode(),
                        truncateForLog(response.body()));
                return GeminiCallResult.failure(
                        "AI_PROVIDER_HTTP_ERROR",
                        "status=%s body=%s".formatted(response.statusCode(), truncateForLog(response.body())));
            }

            GeminiResponse geminiResponse = objectMapper.readValue(response.body(), GeminiResponse.class);
            if (geminiResponse.promptFeedback != null && StringUtils.hasText(geminiResponse.promptFeedback.blockReason)) {
                String detail = "blockReason=%s message=%s".formatted(
                        geminiResponse.promptFeedback.blockReason,
                        geminiResponse.promptFeedback.blockReasonMessage);
                log.warn("Gemini request blocked (schema={}): {}", useResponseSchema, detail);
                return GeminiCallResult.failure("AI_PROVIDER_BLOCKED", detail);
            }

            String text = geminiResponse.firstText();
            if (!StringUtils.hasText(text)) {
                String finishReason = geminiResponse.firstFinishReason();
                log.warn("Gemini returned empty content (schema={}) body={}",
                        useResponseSchema,
                        truncateForLog(response.body()));
                if ("MAX_TOKENS".equalsIgnoreCase(finishReason)) {
                    return GeminiCallResult.failure("AI_PROVIDER_MAX_TOKENS", "finishReason=MAX_TOKENS");
                }
                return GeminiCallResult.failure(
                        "AI_PROVIDER_EMPTY_RESPONSE",
                        "No text in candidates. finishReason=" + finishReason);
            }

            GeminiCallResult parsed = parseFeedbackResult(text, useResponseSchema);
            if (parsed.feedback().isPresent()) {
                return parsed;
            }

            if (allowRepair && "AI_PROVIDER_INVALID_JSON".equals(parsed.errorCode())) {
                GeminiCallResult repaired = requestJsonRepair(text);
                if (repaired.feedback().isPresent()) {
                    return repaired;
                }
            }
            return parsed;
        } catch (IOException ex) {
            log.warn("Gemini feedback parse failed (schema={}): {}", useResponseSchema, ex.getMessage());
            return GeminiCallResult.failure("AI_PROVIDER_INVALID_JSON", ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return GeminiCallResult.failure("AI_PROVIDER_INTERRUPTED", ex.getMessage());
        }
    }

    private URI buildUri() {
        String baseUrl = StringUtils.hasText(properties.baseUrl())
                ? properties.baseUrl()
                : "https://generativelanguage.googleapis.com";
        String model = effectiveModel();

        return URI.create(String.format(
                "%s/v1beta/models/%s:generateContent?key=%s",
                baseUrl,
                model,
                properties.apiKey()
        ));
    }

    private String buildPayload(String prompt, boolean useResponseSchema) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode content = objectMapper.createObjectNode();
        content.put("role", "user");
        ObjectNode part = objectMapper.createObjectNode();
        part.put("text", prompt);
        content.set("parts", objectMapper.valueToTree(List.of(part)));
        root.set("contents", objectMapper.valueToTree(List.of(content)));

        ObjectNode config = objectMapper.createObjectNode();
        config.put("temperature", 0.1d);
        config.put("maxOutputTokens", properties.maxOutputTokens());
        config.put("responseMimeType", "application/json");
        if (useResponseSchema) {
            config.set("responseSchema", buildResponseSchema());
        }
        root.set("generationConfig", config);

        return objectMapper.writeValueAsString(root);
    }

    private ObjectNode buildResponseSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "OBJECT");

        ObjectNode propertiesNode = objectMapper.createObjectNode();

        ObjectNode summary = objectMapper.createObjectNode();
        summary.put("type", "STRING");
        propertiesNode.set("summary", summary);

        ObjectNode strengths = objectMapper.createObjectNode();
        strengths.put("type", "ARRAY");
        ObjectNode strengthsItems = objectMapper.createObjectNode();
        strengthsItems.put("type", "STRING");
        strengths.set("items", strengthsItems);
        propertiesNode.set("strengths", strengths);

        ObjectNode improvements = objectMapper.createObjectNode();
        improvements.put("type", "ARRAY");
        ObjectNode improvementsItems = objectMapper.createObjectNode();
        improvementsItems.put("type", "STRING");
        improvements.set("items", improvementsItems);
        propertiesNode.set("improvements", improvements);

        schema.set("properties", propertiesNode);

        ArrayNode required = objectMapper.createArrayNode();
        required.add("summary");
        required.add("strengths");
        required.add("improvements");
        schema.set("required", required);
        return schema;
    }

    private boolean isUsable(GeminiFeedback feedback) {
        return feedback != null
                && StringUtils.hasText(feedback.summary());
    }

    private String extractJsonObject(String rawText) {
        String trimmed = rawText == null ? "" : rawText.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }

        Pattern fenced = Pattern.compile("```(?:json)?\\s*(\\{.*?})\\s*```", Pattern.DOTALL);
        Matcher fencedMatcher = fenced.matcher(trimmed);
        if (fencedMatcher.find()) {
            return fencedMatcher.group(1);
        }

        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }

        return trimmed;
    }

    private String truncateForLog(String input) {
        if (input == null) {
            return null;
        }
        String normalized = input.replaceAll("\\s+", " ").trim();
        int max = 400;
        return normalized.length() > max ? normalized.substring(0, max) + "..." : normalized;
    }

    private GeminiCallResult parseFeedbackResult(String text, boolean useResponseSchema) {
        try {
            GeminiFeedback feedback = parseFeedback(text);
            if (!isUsable(feedback)) {
                log.warn("Gemini returned unusable feedback (schema={}) text={}",
                        useResponseSchema,
                        truncateForLog(text));
                return GeminiCallResult.failure("AI_PROVIDER_UNUSABLE_JSON", truncateForLog(text));
            }
            return GeminiCallResult.success(feedback);
        } catch (IOException ex) {
            log.warn("Gemini feedback parse failed (schema={}): {}", useResponseSchema, ex.getMessage());
            return GeminiCallResult.failure("AI_PROVIDER_INVALID_JSON", ex.getMessage());
        }
    }

    private GeminiCallResult requestJsonRepair(String malformedResponse) {
        String repairPrompt = """
                Converta o texto abaixo para JSON valido em UMA LINHA, sem markdown, sem comentarios.
                Saida obrigatoria:
                {"summary":"...","strengths":["..."],"improvements":["..."]}
                Regras:
                - Escape aspas internas corretamente.
                - Remova quebras de linha dentro de strings.
                - Se faltar algum campo, complete com texto curto e objetivo.

                Texto a corrigir:
                %s
                """.formatted(malformedResponse);
        return requestFeedback(repairPrompt, false, false);
    }

    private GeminiFeedback parseFeedback(String rawText) throws IOException {
        String extractedJson = extractJsonObject(rawText);
        try {
            return normalize(objectMapper.readValue(extractedJson, GeminiFeedback.class));
        } catch (IOException first) {
            // Common malformed case from providers: unescaped line breaks inside quoted strings.
            String flattened = extractedJson.replace('\r', ' ').replace('\n', ' ');
            try {
                return normalize(lenientObjectMapper.readValue(flattened, GeminiFeedback.class));
            } catch (IOException second) {
                throw second;
            }
        }
    }

    private GeminiFeedback normalize(GeminiFeedback feedback) {
        if (feedback == null) {
            return null;
        }
        String summary = feedback.summary() == null ? "" : feedback.summary().trim();
        List<String> strengths = sanitizeList(feedback.strengths());
        List<String> improvements = sanitizeList(feedback.improvements());
        return new GeminiFeedback(summary, strengths, improvements);
    }

    private List<String> sanitizeList(List<String> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }
        return input.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(5)
                .toList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeminiResponse(List<Candidate> candidates, PromptFeedback promptFeedback) {
        String firstText() {
            if (candidates == null || candidates.isEmpty()) {
                return null;
            }
            for (Candidate candidate : candidates) {
                if (candidate == null || candidate.content == null || candidate.content.parts == null) {
                    continue;
                }
                StringBuilder combined = new StringBuilder();
                for (Part part : candidate.content.parts) {
                    if (part != null && StringUtils.hasText(part.text)) {
                        combined.append(part.text);
                    }
                }
                String text = combined.toString().trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
            return null;
        }

        String firstFinishReason() {
            if (candidates == null || candidates.isEmpty()) {
                return null;
            }
            Candidate candidate = candidates.get(0);
            return candidate == null ? null : candidate.finishReason;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidate(Content content, String finishReason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Content(List<Part> parts) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Part(String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PromptFeedback(String blockReason, String blockReasonMessage) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeminiFeedback(
            String summary,
            List<String> strengths,
            List<String> improvements
    ) {}

    public record GeminiCallResult(Optional<GeminiFeedback> feedback, String errorCode, String errorDetail) {
        static GeminiCallResult success(GeminiFeedback feedback) {
            return new GeminiCallResult(Optional.ofNullable(feedback), null, null);
        }

        static GeminiCallResult failure(String errorCode, String errorDetail) {
            return new GeminiCallResult(Optional.empty(), errorCode, errorDetail);
        }
    }
}
