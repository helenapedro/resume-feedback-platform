package com.pedro.resumeworker.ai.openai;

import java.util.Optional;

record OpenAiCallResult(Optional<String> text, String errorCode, String errorDetail) {

    static OpenAiCallResult success(String text) {
        return new OpenAiCallResult(Optional.ofNullable(text), null, null);
    }

    static OpenAiCallResult failure(String errorCode, String errorDetail) {
        return new OpenAiCallResult(Optional.empty(), errorCode, errorDetail);
    }
}
