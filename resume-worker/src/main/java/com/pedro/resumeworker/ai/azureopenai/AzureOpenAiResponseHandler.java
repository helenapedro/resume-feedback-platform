package com.pedro.resumeworker.ai.azureopenai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.http.HttpResponse;

@Component
@Slf4j
class AzureOpenAiResponseHandler {

    private static final String STOP_FINISH_REASON = "stop";
    private static final String LENGTH_FINISH_REASON = "length";
    private static final String EMPTY_RESPONSE_ERROR_CODE = "AI_PROVIDER_EMPTY_RESPONSE";
    private static final String INCOMPLETE_ERROR_CODE = "AI_PROVIDER_INCOMPLETE";
    private static final String MAX_TOKENS_ERROR_CODE = "AI_PROVIDER_MAX_TOKENS";
    private static final String HTTP_ERROR_CODE = "AI_PROVIDER_HTTP_ERROR";
    private static final String PROVIDER_ERROR_CODE = "AI_PROVIDER_ERROR";
    private static final int MAX_LOG_BODY_CHARS = 1000;

    private final ObjectMapper objectMapper;

    AzureOpenAiResponseHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    AzureOpenAiCallResult handle(HttpResponse<String> response) throws IOException {
        if (isHttpError(response)) {
            String body = truncateForLog(response.body());
            log.warn("Azure OpenAI HTTP error status={} body={}", response.statusCode(), body);
            return AzureOpenAiCallResult.failure(
                    HTTP_ERROR_CODE,
                    "status=%s body=%s".formatted(response.statusCode(), body));
        }

        AzureOpenAiResponse azureResponse = objectMapper.readValue(response.body(), AzureOpenAiResponse.class);
        if (azureResponse.error() != null) {
            return AzureOpenAiCallResult.failure(PROVIDER_ERROR_CODE, azureResponse.error().message());
        }

        return azureResponse.firstChoice()
                .map(this::extractCompletedText)
                .orElseGet(() -> AzureOpenAiCallResult.failure(
                        EMPTY_RESPONSE_ERROR_CODE,
                        "No choices in Azure OpenAI response"));
    }

    private boolean isHttpError(HttpResponse<String> response) {
        return response.statusCode() < 200 || response.statusCode() >= 300;
    }

    private AzureOpenAiCallResult extractCompletedText(AzureOpenAiChoice choice) {
        if (hasIncompleteFinishReason(choice.finishReason())) {
            return AzureOpenAiCallResult.failure(
                    finishReasonErrorCode(choice.finishReason()),
                    "finish_reason=%s".formatted(choice.finishReason()));
        }

        return choice.messageContent()
                .map(AzureOpenAiCallResult::success)
                .orElseGet(() -> AzureOpenAiCallResult.failure(
                        EMPTY_RESPONSE_ERROR_CODE,
                        "No message content in Azure OpenAI response"));
    }

    private boolean hasIncompleteFinishReason(String finishReason) {
        return StringUtils.hasText(finishReason) && !STOP_FINISH_REASON.equalsIgnoreCase(finishReason);
    }

    private String finishReasonErrorCode(String finishReason) {
        return LENGTH_FINISH_REASON.equalsIgnoreCase(finishReason)
                ? MAX_TOKENS_ERROR_CODE
                : INCOMPLETE_ERROR_CODE;
    }

    private String truncateForLog(String value) {
        if (value == null || value.length() <= MAX_LOG_BODY_CHARS) {
            return value;
        }
        return value.substring(0, MAX_LOG_BODY_CHARS) + "...";
    }
}
