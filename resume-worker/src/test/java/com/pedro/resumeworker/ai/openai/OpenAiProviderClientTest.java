package com.pedro.resumeworker.ai.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedro.resumeworker.ai.provider.AiProviderFeedbackCallResult;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiProviderClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void generateFeedbackFailsFastWhenProviderIsDisabled() {
        OpenAiProviderClient client = newClient(
                new OpenAiProperties(false, "", "https://api.openai.com", "gpt-4o-mini", 0.2, 1800));

        AiProviderFeedbackCallResult result = client.generateFeedback("prompt");

        assertTrue(result.feedback().isEmpty());
        assertEquals("AI_PROVIDER_DISABLED", result.errorCode());
    }

    @Test
    void providerModelIncludesProviderPrefix() {
        OpenAiProviderClient client = newClient(
                new OpenAiProperties(false, "", "https://api.openai.com", "gpt-test", 0.2, 1800));

        assertEquals("openai:gpt-test", client.providerModel());
    }

    private OpenAiProviderClient newClient(OpenAiProperties properties) {
        OpenAiSchemaFactory schemaFactory = new OpenAiSchemaFactory(objectMapper);
        OpenAiRequestFactory requestFactory = new OpenAiRequestFactory(properties, objectMapper, schemaFactory);
        OpenAiResponseHandler responseHandler = new OpenAiResponseHandler(objectMapper);
        OpenAiTransport transport = new OpenAiTransport(httpClient, responseHandler);
        OpenAiPayloadMapper payloadMapper = new OpenAiPayloadMapper(objectMapper);

        return new OpenAiProviderClient(properties, requestFactory, transport, payloadMapper);
    }
}
