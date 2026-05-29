package com.pedro.resumeworker.ai.provider;

public interface AiProviderClient {

    AiProviderFeedbackCallResult generateFeedback(String prompt);

    AiProviderProgressCallResult generateProgressAnalysis(String prompt);

    String providerName();

    String effectiveModel();

    default String providerModel() {
        return providerName().toLowerCase() + ":" + effectiveModel();
    }
}
