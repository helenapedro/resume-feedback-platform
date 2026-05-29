package com.pedro.resumeworker.ai.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedro.resumeworker.ai.provider.AiProviderFeedbackCallResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiProviderClientTest {

    @Test
    void generateFeedbackFailsFastWhenProviderIsDisabled() {
        OpenAiProviderClient client = new OpenAiProviderClient(
                new OpenAiProperties(false, "", "https://api.openai.com", "gpt-4o-mini", 0.2, 1800),
                new ObjectMapper());

        AiProviderFeedbackCallResult result = client.generateFeedback("prompt");

        assertTrue(result.feedback().isEmpty());
        assertEquals("AI_PROVIDER_DISABLED", result.errorCode());
    }

    @Test
    void providerModelIncludesProviderPrefix() {
        OpenAiProviderClient client = new OpenAiProviderClient(
                new OpenAiProperties(false, "", "https://api.openai.com", "gpt-test", 0.2, 1800),
                new ObjectMapper());

        assertEquals("openai:gpt-test", client.providerModel());
    }
}
