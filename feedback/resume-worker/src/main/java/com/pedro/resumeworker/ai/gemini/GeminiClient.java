package com.pedro.resumeworker.ai.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

@Component
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

            GeminiFeedback feedback = objectMapper.readValue(text, GeminiFeedback.class);
            return Optional.ofNullable(feedback);
        } catch (IOException ex) {
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
