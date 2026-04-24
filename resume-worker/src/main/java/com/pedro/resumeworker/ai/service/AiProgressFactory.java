package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.common.ai.Language;
import com.pedro.common.ai.mongo.AiFeedbackDocument;
import com.pedro.common.ai.mongo.AiProgressDocument;
import com.pedro.resumeworker.ai.domain.ResumeVersion;
import com.pedro.resumeworker.ai.gemini.GeminiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AiProgressFactory {

    private final String promptVersion;
    private final GeminiClient geminiClient;
    private final ResumeTextExtractor resumeTextExtractor;
    private final AiProgressPromptBuilder promptBuilder;

    public AiProgressFactory(
            @Value("${app.ai-feedback.prompt-version:v1}") String promptVersion,
            GeminiClient geminiClient,
            ResumeTextExtractor resumeTextExtractor,
            AiProgressPromptBuilder promptBuilder) {
        this.promptVersion = promptVersion;
        this.geminiClient = geminiClient;
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

        GeminiClient.GeminiProgressCallResult result = geminiClient.generateProgressAnalysisWithDiagnostics(
                promptBuilder.build(
                        message,
                        currentVersion,
                        previousVersion,
                        currentResumeText,
                        previousResumeText,
                        previousFeedback,
                        language));
        GeminiClient.GeminiProgressAnalysis progress = result.analysis().orElse(null);
        if (progress == null) {
            throw new AiJobDomainException(
                    result.errorCode() == null ? "AI_PROVIDER_EMPTY_PROGRESS_RESPONSE" : result.errorCode(),
                    "Gemini progress failure. jobId=%s resumeVersionId=%s baselineResumeVersionId=%s detail=%s"
                            .formatted(message.jobId(), currentVersion.getId(), previousVersion.getId(),
                                    result.errorDetail()));
        }

        AiProgressDocument doc = new AiProgressDocument();
        doc.setJobId(message.jobId());
        doc.setResumeId(message.resumeId());
        doc.setResumeVersionId(currentVersion.getId());
        doc.setBaselineResumeVersionId(previousVersion.getId());
        doc.setOwnerId(message.ownerId());
        doc.setCreatedAt(Instant.now());
        doc.setModel(geminiClient.effectiveModel());
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
