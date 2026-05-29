package com.pedro.resumeworker.ai.gemini;

import com.pedro.resumeworker.ai.provider.AiFeedbackResult;
import com.pedro.resumeworker.ai.provider.AiProgressResult;
import com.pedro.resumeworker.ai.provider.AiProviderClient;
import com.pedro.resumeworker.ai.provider.AiProviderFeedbackCallResult;
import com.pedro.resumeworker.ai.provider.AiProviderProgressCallResult;
import org.springframework.stereotype.Component;

@Component
public class GeminiAiProviderClient implements AiProviderClient {

    private final GeminiClient geminiClient;

    public GeminiAiProviderClient(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    @Override
    public AiProviderFeedbackCallResult generateFeedback(String prompt) {
        GeminiClient.GeminiCallResult result = geminiClient.generateFeedbackWithDiagnostics(prompt);
        return result.feedback()
                .map(feedback -> AiProviderFeedbackCallResult.success(new AiFeedbackResult(
                        feedback.summary(),
                        feedback.strengths(),
                        feedback.improvements(),
                        providerModel())))
                .orElseGet(() -> AiProviderFeedbackCallResult.failure(result.errorCode(), result.errorDetail()));
    }

    @Override
    public AiProviderProgressCallResult generateProgressAnalysis(String prompt) {
        GeminiClient.GeminiProgressCallResult result = geminiClient.generateProgressAnalysisWithDiagnostics(prompt);
        return result.analysis()
                .map(progress -> AiProviderProgressCallResult.success(new AiProgressResult(
                        progress.summary(),
                        progress.progressStatus(),
                        progress.progressScore(),
                        progress.improvedAreas(),
                        progress.unchangedIssues(),
                        progress.newIssues(),
                        providerModel())))
                .orElseGet(() -> AiProviderProgressCallResult.failure(result.errorCode(), result.errorDetail()));
    }

    @Override
    public String providerName() {
        return "gemini";
    }

    @Override
    public String effectiveModel() {
        return geminiClient.effectiveModel();
    }
}
