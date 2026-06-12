package com.pedro.resumeworker.ai.azureopenai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
record AzureOpenAiResponse(
        AzureOpenAiError error,
        List<AzureOpenAiChoice> choices
) {
    Optional<AzureOpenAiChoice> firstChoice() {
        if (choices == null) {
            return Optional.empty();
        }
        return choices.stream().findFirst();
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
record AzureOpenAiError(String message) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record AzureOpenAiChoice(
        AzureOpenAiMessage message,
        @JsonProperty("finish_reason") String finishReason
) {
    Optional<String> messageContent() {
        return Optional.ofNullable(message)
                .map(AzureOpenAiMessage::content)
                .filter(StringUtils::hasText);
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
record AzureOpenAiMessage(String role, String content) {
}
