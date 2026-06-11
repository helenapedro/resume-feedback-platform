package com.pedro.resumeworker.ai.azureopenai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedro.resumeworker.ai.provider.AiFeedbackResult;
import com.pedro.resumeworker.ai.provider.AiProgressResult;
import com.pedro.resumeworker.ai.provider.AiProviderFeedbackCallResult;
import com.pedro.resumeworker.ai.provider.AiProviderProgressCallResult;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
class AzureOpenAiPayloadMapper {

    private static final String INVALID_JSON_ERROR_CODE = "AI_PROVIDER_INVALID_JSON";

    private final ObjectMapper objectMapper;

    AzureOpenAiPayloadMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    AiProviderFeedbackCallResult toFeedbackResult(AzureOpenAiCallResult result, String providerModel) {
        if (result.text().isEmpty()) {
            return AiProviderFeedbackCallResult.failure(result.errorCode(), result.errorDetail());
        }

        try {
            AzureOpenAiFeedbackPayload payload = objectMapper.readValue(
                    result.text().orElseThrow(),
                    AzureOpenAiFeedbackPayload.class);
            return AiProviderFeedbackCallResult.success(new AiFeedbackResult(
                    normalize(payload.summary()),
                    sanitizeList(payload.strengths()),
                    sanitizeList(payload.improvements()),
                    providerModel));
        } catch (IOException ex) {
            return AiProviderFeedbackCallResult.failure(INVALID_JSON_ERROR_CODE, ex.getMessage());
        }
    }

    AiProviderProgressCallResult toProgressResult(AzureOpenAiCallResult result, String providerModel) {
        if (result.text().isEmpty()) {
            return AiProviderProgressCallResult.failure(result.errorCode(), result.errorDetail());
        }

        try {
            AzureOpenAiProgressPayload payload = objectMapper.readValue(
                    result.text().orElseThrow(),
                    AzureOpenAiProgressPayload.class);
            return AiProviderProgressCallResult.success(new AiProgressResult(
                    normalize(payload.summary()),
                    normalize(payload.progressStatus()),
                    payload.progressScore(),
                    sanitizeList(payload.improvedAreas()),
                    sanitizeList(payload.unchangedIssues()),
                    sanitizeList(payload.newIssues()),
                    providerModel));
        } catch (IOException ex) {
            return AiProviderProgressCallResult.failure(INVALID_JSON_ERROR_CODE, ex.getMessage());
        }
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

    private record AzureOpenAiFeedbackPayload(String summary, List<String> strengths, List<String> improvements) {
    }

    private record AzureOpenAiProgressPayload(
            String summary,
            String progressStatus,
            Integer progressScore,
            List<String> improvedAreas,
            List<String> unchangedIssues,
            List<String> newIssues
    ) {
    }
}
