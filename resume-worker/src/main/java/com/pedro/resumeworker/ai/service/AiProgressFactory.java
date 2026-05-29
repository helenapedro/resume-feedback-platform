package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.common.ai.Language;
import com.pedro.common.ai.mongo.AiFeedbackDocument;
import com.pedro.common.ai.mongo.AiProgressDocument;
import com.pedro.resumeworker.ai.domain.ResumeVersion;
import com.pedro.resumeworker.ai.provider.AiProgressResult;
import com.pedro.resumeworker.ai.provider.AiProviderClient;
import com.pedro.resumeworker.ai.provider.AiProviderProgressCallResult;
import com.pedro.resumeworker.ai.provider.AiProviderRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AiProgressFactory {

    private final String promptVersion;
    private final AiProviderRegistry providerRegistry;
    private final ResumeTextExtractor resumeTextExtractor;
    private final AiProgressPromptBuilder promptBuilder;

    public AiProgressFactory(
            @Value("${app.ai-feedback.prompt-version:v1}") String promptVersion,
            AiProviderRegistry providerRegistry,
            ResumeTextExtractor resumeTextExtractor,
            AiProgressPromptBuilder promptBuilder) {
        this.promptVersion = promptVersion;
        this.providerRegistry = providerRegistry;
        this.resumeTextExtractor = resumeTextExtractor;
        this.promptBuilder = promptBuilder;
    }

    public AiProgressDocument build(
            AiJobRequestedMessage message,
            ResumeVersion currentVersion,
            ResumeVersion previousVersion,
            AiFeedbackDocument previousFeedback) {
        String currentResumeText = resumeTextExtractor.extract(currentVersion).orElse("");
        String previousResumeText = resumeTextExtractor.extract(previousVersion).orElse("");

        Language language = message.language() == null ? Language.EN : message.language();

        AiProviderClient providerClient = providerRegistry.activeClient();
        AiProviderProgressCallResult result = providerClient.generateProgressAnalysis(
                promptBuilder.build(
                        message,
                        currentVersion,
                        previousVersion,
                        currentResumeText,
                        previousResumeText,
                        previousFeedback,
                        language));
        AiProgressResult progress = result.progress().orElse(null);
        if (progress == null) {
            throw new AiJobDomainException(
                    result.errorCode() == null ? "AI_PROVIDER_EMPTY_PROGRESS_RESPONSE" : result.errorCode(),
                    "AI provider progress failure. provider=%s jobId=%s resumeVersionId=%s baselineResumeVersionId=%s detail=%s"
                            .formatted(providerClient.providerName(), message.jobId(), currentVersion.getId(), previousVersion.getId(),
                                    result.errorDetail()));
        }

        AiProgressDocument doc = new AiProgressDocument();
        doc.setJobId(message.jobId());
        doc.setResumeId(message.resumeId());
        doc.setResumeVersionId(currentVersion.getId());
        doc.setBaselineResumeVersionId(previousVersion.getId());
        doc.setOwnerId(message.ownerId());
        doc.setCreatedAt(Instant.now());
        doc.setModel(progress.providerModel());
        doc.setPromptVersion(promptVersion);
        doc.setSummary(progress.summary());
        doc.setProgressStatus(progress.progressStatus());
        doc.setProgressScore(progress.progressScore());
        doc.setImprovedAreas(progress.improvedAreas());
        doc.setUnchangedIssues(progress.unchangedIssues());
        doc.setNewIssues(progress.newIssues());
        return doc;
    }
}
