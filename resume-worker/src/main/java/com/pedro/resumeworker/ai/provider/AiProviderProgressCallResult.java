package com.pedro.resumeworker.ai.provider;

import java.util.Optional;

public record AiProviderProgressCallResult(
        Optional<AiProgressResult> progress,
        String errorCode,
        String errorDetail
) {
    public static AiProviderProgressCallResult success(AiProgressResult progress) {
        return new AiProviderProgressCallResult(Optional.ofNullable(progress), null, null);
    }

    public static AiProviderProgressCallResult failure(String errorCode, String errorDetail) {
        return new AiProviderProgressCallResult(Optional.empty(), errorCode, errorDetail);
    }
}
