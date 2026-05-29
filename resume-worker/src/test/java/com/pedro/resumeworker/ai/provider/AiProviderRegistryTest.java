package com.pedro.resumeworker.ai.provider;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiProviderRegistryTest {

    @Test
    void activeClientSelectsConfiguredProviderIgnoringCase() {
        AiProviderClient gemini = stubClient("gemini");
        AiProviderClient openai = stubClient("openai");
        AiProviderRegistry registry = new AiProviderRegistry("OPENAI", List.of(gemini, openai));

        assertEquals(openai, registry.activeClient());
    }

    @Test
    void activeClientFailsWhenConfiguredProviderIsUnavailable() {
        AiProviderRegistry registry = new AiProviderRegistry("openai", List.of(stubClient("gemini")));

        assertThrows(IllegalStateException.class, registry::activeClient);
    }

    private AiProviderClient stubClient(String providerName) {
        return new AiProviderClient() {
            @Override
            public AiProviderFeedbackCallResult generateFeedback(String prompt) {
                return AiProviderFeedbackCallResult.failure("NOT_IMPLEMENTED", "test stub");
            }

            @Override
            public AiProviderProgressCallResult generateProgressAnalysis(String prompt) {
                return AiProviderProgressCallResult.failure("NOT_IMPLEMENTED", "test stub");
            }

            @Override
            public String providerName() {
                return providerName;
            }

            @Override
            public String effectiveModel() {
                return "test-model";
            }
        };
    }
}
