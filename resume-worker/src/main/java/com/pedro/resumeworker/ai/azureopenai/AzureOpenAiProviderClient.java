package com.pedro.resumeworker.ai.azureopenai;

import com.pedro.resumeworker.ai.provider.AiProviderClient;
import com.pedro.resumeworker.ai.provider.AiProviderFeedbackCallResult;
import com.pedro.resumeworker.ai.provider.AiProviderProgressCallResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class AzureOpenAiProviderClient implements AiProviderClient {

    private static final String PROVIDER_NAME = "azure-openai";
    private static final String DISABLED_ERROR_CODE = "AI_PROVIDER_DISABLED";
    private static final String INVALID_RESPONSE_ERROR_CODE = "AI_PROVIDER_INVALID_RESPONSE";
    private static final String DISABLED_ERROR_MESSAGE =
            "Azure OpenAI is disabled or endpoint, deployment, or API key is missing";

    private final AzureOpenAiProperties properties;
    private final AzureOpenAiRequestFactory requestFactory;
    private final AzureOpenAiTransport transport;
    private final AzureOpenAiPayloadMapper payloadMapper;

    public AzureOpenAiProviderClient(
            AzureOpenAiProperties properties,
            AzureOpenAiRequestFactory requestFactory,
            AzureOpenAiTransport transport,
            AzureOpenAiPayloadMapper payloadMapper) {
        this.properties = properties;
        this.requestFactory = requestFactory;
        this.transport = transport;
        this.payloadMapper = payloadMapper;
        log.info("Azure OpenAI provider configured deployment={} apiVersion={} maxOutputTokens={} temperature={}",
                effectiveModel(),
                properties.effectiveApiVersion(),
                properties.maxOutputTokens(),
                properties.temperature());
    }

    @Override
    public AiProviderFeedbackCallResult generateFeedback(String prompt) {
        if (!isEnabled()) {
            return AiProviderFeedbackCallResult.failure(DISABLED_ERROR_CODE, DISABLED_ERROR_MESSAGE);
        }

        return payloadMapper.toFeedbackResult(
                requestFeedback(prompt),
                providerModel());
    }

    @Override
    public AiProviderProgressCallResult generateProgressAnalysis(String prompt) {
        if (!isEnabled()) {
            return AiProviderProgressCallResult.failure(DISABLED_ERROR_CODE, DISABLED_ERROR_MESSAGE);
        }

        return payloadMapper.toProgressResult(
                requestProgress(prompt),
                providerModel());
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public String effectiveModel() {
        return properties.effectiveDeployment();
    }

    private boolean isEnabled() {
        return properties.hasRequiredProviderConfig();
    }

    private AzureOpenAiCallResult requestFeedback(String prompt) {
        try {
            return transport.send(requestFactory.feedbackRequest(prompt));
        } catch (IOException ex) {
            return AzureOpenAiCallResult.failure(INVALID_RESPONSE_ERROR_CODE, ex.getMessage());
        }
    }

    private AzureOpenAiCallResult requestProgress(String prompt) {
        try {
            return transport.send(requestFactory.progressRequest(prompt));
        } catch (IOException ex) {
            return AzureOpenAiCallResult.failure(INVALID_RESPONSE_ERROR_CODE, ex.getMessage());
        }
    }
}
