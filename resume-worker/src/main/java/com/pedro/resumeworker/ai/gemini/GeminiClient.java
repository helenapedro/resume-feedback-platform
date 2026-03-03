package com.pedro.resumeworker.ai.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class GeminiClient {

    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeminiClient(GeminiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public Optional<GeminiFeedback> generateFeedback(String prompt) {
        if (!properties.enabled() || !StringUtils.hasText(properties.apiKey())) {
            return Optional.empty();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(buildUri())
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(buildPayload(prompt)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }

            GeminiResponse geminiResponse = objectMapper.readValue(response.body(), GeminiResponse.class);
            String text = geminiResponse.firstText();
            if (!StringUtils.hasText(text)) {
                return Optional.empty();
            }

            GeminiFeedback feedback = objectMapper.readValue(extractJsonObject(text), GeminiFeedback.class);
            if (!isUsable(feedback)) {
                return Optional.empty();
            }
            return Optional.ofNullable(feedback);
        } catch (IOException ex) {
            log.warn("Gemini feedback parse failed: {}", ex.getMessage());
            return Optional.empty();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private URI buildUri() {
        String baseUrl = StringUtils.hasText(properties.baseUrl())
                ? properties.baseUrl()
                : "https://generativelanguage.googleapis.com";
        String model = StringUtils.hasText(properties.model())
                ? properties.model()
                : "gemini-1.5-flash";

        return URI.create(String.format(
                "%s/v1beta/models/%s:generateContent?key=%s",
                baseUrl,
                model,
                properties.apiKey()
        ));
    }

    private String buildPayload(String prompt) throws IOException {
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
        root.set("generationConfig", config);

        return objectMapper.writeValueAsString(root);
    }

    private boolean isUsable(GeminiFeedback feedback) {
        return feedback != null
                && StringUtils.hasText(feedback.summary())
                && feedback.strengths() != null
                && !feedback.strengths().isEmpty()
                && feedback.improvements() != null
                && !feedback.improvements().isEmpty();
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeminiResponse(List<Candidate> candidates) {
        String firstText() {
            if (candidates == null || candidates.isEmpty()) {
                return null;
            }
            Candidate candidate = candidates.get(0);
            if (candidate.content == null || candidate.content.parts == null || candidate.content.parts.isEmpty()) {
                return null;
            }
            return candidate.content.parts.get(0).text;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidate(Content content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Content(List<Part> parts) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Part(String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeminiFeedback(
            String summary,
            List<String> strengths,
            List<String> improvements
    ) {}
}
