package com.pedro.resumeworker.ai.azureopenai;

import java.util.Optional;

record AzureOpenAiCallResult(Optional<String> text, String errorCode, String errorDetail) {

    static AzureOpenAiCallResult success(String text) {
        return new AzureOpenAiCallResult(Optional.ofNullable(text), null, null);
    }

    static AzureOpenAiCallResult failure(String errorCode, String errorDetail) {
        return new AzureOpenAiCallResult(Optional.empty(), errorCode, errorDetail);
    }
}
