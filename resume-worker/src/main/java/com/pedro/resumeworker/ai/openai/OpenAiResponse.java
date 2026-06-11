package com.pedro.resumeworker.ai.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
record OpenAiResponse(
        String status,
        OpenAiError error,
        @JsonProperty("incomplete_details") OpenAiIncompleteDetails incompleteDetails,
        List<OpenAiOutputItem> output
) {
    Optional<String> firstOutputText() {
        if (output == null) {
            return Optional.empty();
        }
        return output.stream()
                .filter(item -> item.content() != null)
                .flatMap(item -> item.content().stream())
                .filter(content -> "output_text".equals(content.type()))
                .map(OpenAiContentItem::text)
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
                .map(OpenAiContentItem::refusal)
                .filter(StringUtils::hasText)
                .findFirst();
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
record OpenAiError(String message) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record OpenAiIncompleteDetails(String reason) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record OpenAiOutputItem(String type, List<OpenAiContentItem> content) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record OpenAiContentItem(String type, String text, String refusal) {
}
