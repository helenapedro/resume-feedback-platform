package com.pedro.resumeworker.ai.provider;

import java.util.Optional;

public record AiProviderFeedbackCallResult(
        Optional<AiFeedbackResult> feedback,
        String errorCode,
        String errorDetail
) {
    public static AiProviderFeedbackCallResult success(AiFeedbackResult feedback) {
        return new AiProviderFeedbackCallResult(Optional.ofNullable(feedback), null, null);
    }

    public static AiProviderFeedbackCallResult failure(String errorCode, String errorDetail) {
        return new AiProviderFeedbackCallResult(Optional.empty(), errorCode, errorDetail);
    }
}
