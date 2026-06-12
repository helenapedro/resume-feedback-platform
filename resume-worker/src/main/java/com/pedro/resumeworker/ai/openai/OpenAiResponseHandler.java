package com.pedro.resumeworker.ai.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.http.HttpResponse;

@Component
@Slf4j
class OpenAiResponseHandler {

    private static final String COMPLETED_STATUS = "completed";
    private static final String MAX_OUTPUT_TOKENS_REASON = "max_output_tokens";
    private static final String HTTP_ERROR_CODE = "AI_PROVIDER_HTTP_ERROR";
    private static final String PROVIDER_ERROR_CODE = "AI_PROVIDER_ERROR";
    private static final String INCOMPLETE_ERROR_CODE = "AI_PROVIDER_INCOMPLETE";
    private static final String MAX_TOKENS_ERROR_CODE = "AI_PROVIDER_MAX_TOKENS";
    private static final String BLOCKED_ERROR_CODE = "AI_PROVIDER_BLOCKED";
    private static final String EMPTY_RESPONSE_ERROR_CODE = "AI_PROVIDER_EMPTY_RESPONSE";
    private static final int MAX_LOG_BODY_CHARS = 1000;

    private final ObjectMapper objectMapper;

    OpenAiResponseHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    OpenAiCallResult handle(HttpResponse<String> response) throws IOException {
        if (isHttpError(response)) {
            String body = truncateForLog(response.body());
            log.warn("OpenAI HTTP error status={} body={}", response.statusCode(), body);
            return OpenAiCallResult.failure(
                    HTTP_ERROR_CODE,
                    "status=%s body=%s".formatted(response.statusCode(), body));
        }

        OpenAiResponse openAiResponse = objectMapper.readValue(response.body(), OpenAiResponse.class);
        if (openAiResponse.error() != null) {
            return OpenAiCallResult.failure(PROVIDER_ERROR_CODE, openAiResponse.error().message());
        }
        if (!COMPLETED_STATUS.equalsIgnoreCase(openAiResponse.status())) {
            return incompleteResult(openAiResponse);
        }
        return openAiResponse.firstRefusal()
                .map(refusal -> OpenAiCallResult.failure(BLOCKED_ERROR_CODE, refusal))
                .orElseGet(() -> firstOutputResult(openAiResponse));
    }

    private boolean isHttpError(HttpResponse<String> response) {
        return response.statusCode() < 200 || response.statusCode() >= 300;
    }

    private OpenAiCallResult incompleteResult(OpenAiResponse response) {
        String reason = response.incompleteDetails() == null
                ? response.status()
                : response.incompleteDetails().reason();
        String code = MAX_OUTPUT_TOKENS_REASON.equalsIgnoreCase(reason)
                ? MAX_TOKENS_ERROR_CODE
                : INCOMPLETE_ERROR_CODE;
        return OpenAiCallResult.failure(code, "status=%s reason=%s".formatted(response.status(), reason));
    }

    private OpenAiCallResult firstOutputResult(OpenAiResponse response) {
        return response.firstOutputText()
                .map(OpenAiCallResult::success)
                .orElseGet(() -> OpenAiCallResult.failure(
                        EMPTY_RESPONSE_ERROR_CODE,
                        "No output_text in OpenAI response"));
    }

    private String truncateForLog(String value) {
        if (value == null || value.length() <= MAX_LOG_BODY_CHARS) {
            return value;
        }
        return value.substring(0, MAX_LOG_BODY_CHARS) + "...";
    }
}
