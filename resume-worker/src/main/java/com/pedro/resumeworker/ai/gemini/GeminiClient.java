package com.pedro.resumeworker.ai.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
    private final GeminiRequestFactory requestFactory;

    public GeminiClient(
            GeminiProperties properties,
            ObjectMapper objectMapper,
            HttpClient geminiHttpClient,
            GeminiRequestFactory requestFactory) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.lenientObjectMapper = JsonMapper.builder()
                .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
                .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .build();
        this.httpClient = geminiHttpClient;
        this.requestFactory = requestFactory;
        log.info("Gemini client configured model={} maxOutputTokens={} temperature={}",
                effectiveModel(),
                properties.maxOutputTokens(),
                properties.temperature());
    }

    public Optional<GeminiFeedback> generateFeedback(String prompt) {
        return generateFeedbackWithDiagnostics(prompt).feedback();
    }

    public GeminiCallResult generateFeedbackWithDiagnostics(String prompt) {
        if (!properties.hasRequiredProviderConfig()) {
            return GeminiCallResult.failure("AI_PROVIDER_DISABLED", "Gemini is disabled or API key is missing");
        }

        GeminiCallResult strictAttempt = requestFeedback(prompt, true, true);
        if (strictAttempt.feedback().isPresent()) {
            return strictAttempt;
        }
        if (isTerminalProviderFailure(strictAttempt.errorCode())) {
            return strictAttempt;
        }

        // Fallback attempt: remove schema constraints to recover from provider-side structured output issues.
        GeminiCallResult fallbackAttempt = requestFeedback(prompt, false, true);
        if (fallbackAttempt.feedback().isPresent()) {
            return fallbackAttempt;
        }
        if (isTerminalProviderFailure(fallbackAttempt.errorCode())) {
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

    public GeminiProgressCallResult generateProgressAnalysisWithDiagnostics(String prompt) {
        if (!properties.hasRequiredProviderConfig()) {
            return GeminiProgressCallResult.failure("AI_PROVIDER_DISABLED", "Gemini is disabled or API key is missing");
        }

        GeminiProgressCallResult strictAttempt = requestProgressAnalysis(prompt, true, true);
        if (strictAttempt.analysis().isPresent()) {
            return strictAttempt;
        }
        if (isTerminalProviderFailure(strictAttempt.errorCode())) {
            return strictAttempt;
        }

        GeminiProgressCallResult fallbackAttempt = requestProgressAnalysis(prompt, false, true);
        if (fallbackAttempt.analysis().isPresent()) {
            return fallbackAttempt;
        }
        if (isTerminalProviderFailure(fallbackAttempt.errorCode())) {
            return fallbackAttempt;
        }
        if ("AI_PROVIDER_INVALID_JSON".equals(fallbackAttempt.errorCode())) {
            GeminiProgressCallResult compactJsonAttempt = requestProgressAnalysis(
                    prompt + "\n\nIMPORTANT: Return strictly valid JSON in ONE LINE only. No markdown. No comments.",
                    false,
                    true);
            if (compactJsonAttempt.analysis().isPresent()) {
                return compactJsonAttempt;
            }
            return compactJsonAttempt.errorCode() != null ? compactJsonAttempt : fallbackAttempt;
        }
        return fallbackAttempt.errorCode() != null ? fallbackAttempt : strictAttempt;
    }

    public String effectiveModel() {
        return properties.effectiveModel();
    }

    private GeminiCallResult requestFeedback(String prompt, boolean useResponseSchema, boolean allowRepair) {
        try {
            HttpRequest request = requestFactory.feedbackRequest(prompt, useResponseSchema);

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
            String finishReason = geminiResponse.firstFinishReason();
            if (!StringUtils.hasText(text)) {
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
            if ("MAX_TOKENS".equalsIgnoreCase(finishReason)) {
                log.warn("Gemini feedback hit max output tokens (schema={} maxOutputTokens={}) text={}",
                        useResponseSchema,
                        properties.maxOutputTokens(),
                        truncateForLog(text));
                return GeminiCallResult.failure("AI_PROVIDER_MAX_TOKENS", "finishReason=MAX_TOKENS");
            }
            if (looksTruncatedJson(text)) {
                log.warn("Gemini feedback returned truncated JSON (schema={}) text={}",
                        useResponseSchema,
                        truncateForLog(text));
                return GeminiCallResult.failure("AI_PROVIDER_TRUNCATED_JSON", truncateForLog(text));
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

    private GeminiProgressCallResult requestProgressAnalysis(String prompt, boolean useResponseSchema, boolean allowRepair) {
        try {
            HttpRequest request = requestFactory.progressRequest(prompt, useResponseSchema);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Gemini HTTP error for progress (schema={}) status={} body={}",
                        useResponseSchema,
                        response.statusCode(),
                        truncateForLog(response.body()));
                return GeminiProgressCallResult.failure(
                        "AI_PROVIDER_HTTP_ERROR",
                        "status=%s body=%s".formatted(response.statusCode(), truncateForLog(response.body())));
            }

            GeminiResponse geminiResponse = objectMapper.readValue(response.body(), GeminiResponse.class);
            if (geminiResponse.promptFeedback != null && StringUtils.hasText(geminiResponse.promptFeedback.blockReason)) {
                String detail = "blockReason=%s message=%s".formatted(
                        geminiResponse.promptFeedback.blockReason,
                        geminiResponse.promptFeedback.blockReasonMessage);
                log.warn("Gemini progress request blocked (schema={}): {}", useResponseSchema, detail);
                return GeminiProgressCallResult.failure("AI_PROVIDER_BLOCKED", detail);
            }

            String text = geminiResponse.firstText();
            String finishReason = geminiResponse.firstFinishReason();
            if (!StringUtils.hasText(text)) {
                log.warn("Gemini returned empty progress content (schema={}) body={}",
                        useResponseSchema,
                        truncateForLog(response.body()));
                if ("MAX_TOKENS".equalsIgnoreCase(finishReason)) {
                    return GeminiProgressCallResult.failure("AI_PROVIDER_MAX_TOKENS", "finishReason=MAX_TOKENS");
                }
                return GeminiProgressCallResult.failure(
                        "AI_PROVIDER_EMPTY_RESPONSE",
                        "No text in candidates. finishReason=" + finishReason);
            }
            if ("MAX_TOKENS".equalsIgnoreCase(finishReason)) {
                log.warn("Gemini progress hit max output tokens (schema={} maxOutputTokens={}) text={}",
                        useResponseSchema,
                        properties.maxOutputTokens(),
                        truncateForLog(text));
                return GeminiProgressCallResult.failure("AI_PROVIDER_MAX_TOKENS", "finishReason=MAX_TOKENS");
            }
            if (looksTruncatedJson(text)) {
                log.warn("Gemini progress returned truncated JSON (schema={}) text={}",
                        useResponseSchema,
                        truncateForLog(text));
                return GeminiProgressCallResult.failure("AI_PROVIDER_TRUNCATED_JSON", truncateForLog(text));
            }

            GeminiProgressCallResult parsed = parseProgressResult(text, useResponseSchema);
            if (parsed.analysis().isPresent()) {
                return parsed;
            }

            if (allowRepair && "AI_PROVIDER_INVALID_JSON".equals(parsed.errorCode())) {
                GeminiProgressCallResult repaired = requestProgressJsonRepair(text);
                if (repaired.analysis().isPresent()) {
                    return repaired;
                }
            }
            return parsed;
        } catch (IOException ex) {
            log.warn("Gemini progress parse failed (schema={}): {}", useResponseSchema, ex.getMessage());
            return GeminiProgressCallResult.failure("AI_PROVIDER_INVALID_JSON", ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return GeminiProgressCallResult.failure("AI_PROVIDER_INTERRUPTED", ex.getMessage());
        }
    }

    private boolean isTerminalProviderFailure(String errorCode) {
        return "AI_PROVIDER_BLOCKED".equals(errorCode)
                || "AI_PROVIDER_MAX_TOKENS".equals(errorCode)
                || "AI_PROVIDER_TRUNCATED_JSON".equals(errorCode);
    }

    private boolean looksTruncatedJson(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String trimmed = text.trim();
        return trimmed.startsWith("{") && !trimmed.endsWith("}");
    }

    private boolean isUsable(GeminiFeedback feedback) {
        return feedback != null
                && StringUtils.hasText(feedback.summary())
                && feedback.strengths() != null
                && feedback.strengths().size() >= 3
                && feedback.improvements() != null
                && feedback.improvements().size() >= 3
                && feedback.strengths().stream().allMatch(this::isSpecificFeedbackItem)
                && feedback.improvements().stream().allMatch(this::isSpecificFeedbackItem);
    }

    private boolean isUsable(GeminiProgressAnalysis analysis) {
        return analysis != null
                && StringUtils.hasText(analysis.summary())
                && StringUtils.hasText(analysis.progressStatus())
                && analysis.progressScore() != null
                && isConsistentProgressScore(analysis.progressStatus(), analysis.progressScore())
                && isConsistentProgressLists(analysis);
    }

    private boolean isSpecificFeedbackItem(String item) {
        return wordCount(item) >= 10 && item.contains(":");
    }

    private boolean isSpecificProgressItem(String item) {
        return wordCount(item) >= 8 && item.contains(":");
    }

    private boolean isConsistentProgressLists(GeminiProgressAnalysis analysis) {
        boolean listsAreSpecific = analysis.improvedAreas().stream().allMatch(this::isSpecificProgressItem)
                && analysis.unchangedIssues().stream().allMatch(this::isSpecificProgressItem)
                && analysis.newIssues().stream().allMatch(this::isSpecificProgressItem);
        if (!listsAreSpecific) {
            return false;
        }
        String normalizedStatus = analysis.progressStatus() == null ? "" : analysis.progressStatus().trim().toUpperCase();
        return !"IMPROVED".equals(normalizedStatus) || !analysis.improvedAreas().isEmpty();
    }

    private boolean isConsistentProgressScore(String progressStatus, Integer progressScore) {
        if (progressScore == null) {
            return false;
        }
        String normalizedStatus = progressStatus == null ? "" : progressStatus.trim().toUpperCase();
        if ("IMPROVED".equals(normalizedStatus)) {
            return progressScore > 0;
        }
        if ("DECLINED".equals(normalizedStatus)) {
            return progressScore == 0;
        }
        return progressScore >= 0 && progressScore <= 100;
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

    private GeminiProgressCallResult parseProgressResult(String text, boolean useResponseSchema) {
        try {
            GeminiProgressAnalysis analysis = parseProgressAnalysis(text);
            if (!isUsable(analysis)) {
                log.warn("Gemini returned unusable progress (schema={}) text={}",
                        useResponseSchema,
                        truncateForLog(text));
                return GeminiProgressCallResult.failure("AI_PROVIDER_UNUSABLE_JSON", truncateForLog(text));
            }
            return GeminiProgressCallResult.success(analysis);
        } catch (IOException ex) {
            log.warn("Gemini progress parse failed (schema={}): {}", useResponseSchema, ex.getMessage());
            return GeminiProgressCallResult.failure("AI_PROVIDER_INVALID_JSON", ex.getMessage());
        }
    }

    private GeminiCallResult requestJsonRepair(String malformedResponse) {
        String repairPrompt = """
                Convert the text below into valid JSON in ONE LINE, with no markdown and no comments.
                Write every text value in English only.
                Required output:
                {"summary":"...","strengths":["3 specific English items"],"improvements":["3 specific English items"]}
                Rules:
                - Escape internal quotes correctly.
                - Remove line breaks inside strings.
                - If a field is missing, complete it with concise, resume-specific English text.

                Text to repair:
                %s
                """.formatted(malformedResponse);
        return requestFeedback(repairPrompt, false, false);
    }

    private GeminiProgressCallResult requestProgressJsonRepair(String malformedResponse) {
        String repairPrompt = """
                Convert the text below into valid JSON in ONE LINE, with no markdown and no comments.
                Write every text value in English only.
                Required output:
                {"summary":"...","progressStatus":"IMPROVED","progressScore":75,"improvedAreas":["..."],"unchangedIssues":["..."],"newIssues":["..."]}
                Rules:
                - progressStatus must be IMPROVED, UNCHANGED, or DECLINED.
                - progressScore must be greater than 0 when progressStatus is IMPROVED.
                - Escape internal quotes correctly.
                - Remove line breaks inside strings.
                - If a field is missing, complete it with concise, resume-specific English text.

                Text to repair:
                %s
                """.formatted(malformedResponse);
        return requestProgressAnalysis(repairPrompt, false, false);
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

    private GeminiProgressAnalysis parseProgressAnalysis(String rawText) throws IOException {
        String extractedJson = extractJsonObject(rawText);
        try {
            return normalize(objectMapper.readValue(extractedJson, GeminiProgressAnalysis.class));
        } catch (IOException first) {
            String flattened = extractedJson.replace('\r', ' ').replace('\n', ' ');
            try {
                return normalize(lenientObjectMapper.readValue(flattened, GeminiProgressAnalysis.class));
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
        List<String> strengths = sanitizeFeedbackList(feedback.strengths());
        List<String> improvements = sanitizeFeedbackList(feedback.improvements());
        return new GeminiFeedback(summary, strengths, improvements);
    }

    private GeminiProgressAnalysis normalize(GeminiProgressAnalysis analysis) {
        if (analysis == null) {
            return null;
        }
        String summary = analysis.summary() == null ? "" : analysis.summary().trim();
        String progressStatus = analysis.progressStatus() == null ? "" : analysis.progressStatus().trim();
        Integer progressScore = analysis.progressScore() == null
                ? null
                : Math.max(0, Math.min(100, analysis.progressScore()));
        List<String> improvedAreas = sanitizeProgressList(analysis.improvedAreas());
        List<String> unchangedIssues = sanitizeProgressList(analysis.unchangedIssues());
        List<String> newIssues = sanitizeProgressList(analysis.newIssues());
        return new GeminiProgressAnalysis(summary, progressStatus, progressScore, improvedAreas, unchangedIssues, newIssues);
    }

    private List<String> sanitizeFeedbackList(List<String> input) {
        return sanitizeList(input).stream()
                .filter(this::isSpecificFeedbackItem)
                .toList();
    }

    private List<String> sanitizeProgressList(List<String> input) {
        return sanitizeList(input).stream()
                .filter(this::isSpecificProgressItem)
                .toList();
    }

    private List<String> sanitizeList(List<String> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }
        return input.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(item -> !isEmptyListPlaceholder(item))
                .distinct()
                .limit(5)
                .toList();
    }

    private boolean isEmptyListPlaceholder(String item) {
        String normalized = item == null ? "" : item.trim().toLowerCase();
        return normalized.equals("none")
                || normalized.equals("none identified")
                || normalized.equals("no issues")
                || normalized.equals("no new issues")
                || normalized.equals("no items")
                || normalized.equals("not applicable")
                || normalized.equals("n/a");
    }

    private int wordCount(String item) {
        if (!StringUtils.hasText(item)) {
            return 0;
        }
        return item.trim().split("\\s+").length;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeminiProgressAnalysis(
            String summary,
            String progressStatus,
            Integer progressScore,
            List<String> improvedAreas,
            List<String> unchangedIssues,
            List<String> newIssues
    ) {}

    public record GeminiCallResult(Optional<GeminiFeedback> feedback, String errorCode, String errorDetail) {
        static GeminiCallResult success(GeminiFeedback feedback) {
            return new GeminiCallResult(Optional.ofNullable(feedback), null, null);
        }

        static GeminiCallResult failure(String errorCode, String errorDetail) {
            return new GeminiCallResult(Optional.empty(), errorCode, errorDetail);
        }
    }

    public record GeminiProgressCallResult(Optional<GeminiProgressAnalysis> analysis, String errorCode, String errorDetail) {
        static GeminiProgressCallResult success(GeminiProgressAnalysis analysis) {
            return new GeminiProgressCallResult(Optional.ofNullable(analysis), null, null);
        }

        static GeminiProgressCallResult failure(String errorCode, String errorDetail) {
            return new GeminiProgressCallResult(Optional.empty(), errorCode, errorDetail);
        }
    }
}
