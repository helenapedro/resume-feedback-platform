package com.pedro.resumeworker.ai.azureopenai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedro.resumeworker.ai.provider.AiProviderFeedbackCallResult;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AzureOpenAiProviderClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void generateFeedbackFailsFastWhenProviderIsDisabled() {
        AzureOpenAiProviderClient client = newClient(disabledProperties("gpt-4o-mini"));

        AiProviderFeedbackCallResult result = client.generateFeedback("prompt");

        assertTrue(result.feedback().isEmpty());
        assertEquals("AI_PROVIDER_DISABLED", result.errorCode());
    }

    @Test
    void providerModelIncludesProviderPrefix() {
        AzureOpenAiProviderClient client = newClient(disabledProperties("resume-review-deployment"));

        assertEquals("azure-openai:resume-review-deployment", client.providerModel());
    }

    @Test
    void propertiesReportMissingProviderConfigWhenApiKeyIsBlank() {
        AzureOpenAiProperties properties = disabledProperties("resume-review-deployment");

        assertFalse(properties.hasRequiredProviderConfig());
    }

    @Test
    void propertiesUseDefaultApiVersionWhenMissing() {
        AzureOpenAiProperties properties = new AzureOpenAiProperties(
                true,
                "api-key",
                "https://example.openai.azure.com",
                "resume-review-deployment",
                "",
                0.2,
                1800);

        assertEquals("2024-08-01-preview", properties.effectiveApiVersion());
    }

    private AzureOpenAiProviderClient newClient(AzureOpenAiProperties properties) {
        AzureOpenAiSchemaFactory schemaFactory = new AzureOpenAiSchemaFactory(objectMapper);
        AzureOpenAiRequestFactory requestFactory = new AzureOpenAiRequestFactory(
                properties,
                objectMapper,
                schemaFactory);
        AzureOpenAiResponseHandler responseHandler = new AzureOpenAiResponseHandler(objectMapper);
        AzureOpenAiTransport transport = new AzureOpenAiTransport(httpClient, responseHandler);
        AzureOpenAiPayloadMapper payloadMapper = new AzureOpenAiPayloadMapper(objectMapper);

        return new AzureOpenAiProviderClient(
                properties,
                requestFactory,
                transport,
                payloadMapper);
    }

    private AzureOpenAiProperties disabledProperties(String deployment) {
        return new AzureOpenAiProperties(
                false,
                "",
                "https://example.openai.azure.com",
                deployment,
                "2024-08-01-preview",
                0.2,
                1800);
    }
}
