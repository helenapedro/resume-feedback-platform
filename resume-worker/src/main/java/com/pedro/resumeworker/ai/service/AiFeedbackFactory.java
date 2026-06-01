package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.common.ai.Language;
import com.pedro.common.ai.mongo.AiFeedbackDocument;
import com.pedro.resumeworker.ai.domain.ResumeVersion;
import com.pedro.resumeworker.ai.provider.AiFeedbackResult;
import com.pedro.resumeworker.ai.provider.AiProviderClient;
import com.pedro.resumeworker.ai.provider.AiProviderFeedbackCallResult;
import com.pedro.resumeworker.ai.provider.AiProviderRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiFeedbackFactory {

    private final AiProviderRegistry providerRegistry;
    private final ResumeTextExtractor resumeTextExtractor;
    private final AiFeedbackPromptBuilder promptBuilder;
    private final AiFeedbackDocumentMapper documentMapper;
    private final ResumeLanguageDetector languageDetector;
    private final ResumeDocumentClassifier documentClassifier;

    public AiFeedbackFactory(
            AiProviderRegistry providerRegistry,
            ResumeTextExtractor resumeTextExtractor,
            AiFeedbackPromptBuilder promptBuilder,
            AiFeedbackDocumentMapper documentMapper,
            ResumeLanguageDetector languageDetector,
            ResumeDocumentClassifier documentClassifier) {
        this.providerRegistry = providerRegistry;
        this.resumeTextExtractor = resumeTextExtractor;
        this.promptBuilder = promptBuilder;
        this.documentMapper = documentMapper;
        this.languageDetector = languageDetector;
        this.documentClassifier = documentClassifier;
    }

    public AiFeedbackDocument build(AiJobRequestedMessage message, ResumeVersion resumeVersion) {
        ResumeTextExtractor.ResumeTextExtraction extraction = resumeTextExtractor.extractWithMetadata(resumeVersion).orElse(null);
        String resumeText = extraction == null ? "" : extraction.text();
        if (!StringUtils.hasText(resumeText)) {
            throw new AiJobDomainException(
                    "RESUME_TEXT_NOT_EXTRACTED",
                    "We could not read enough text from this PDF to generate feedback. Please upload a text-based resume/CV PDF.");
        }
        if (!documentClassifier.isLikelyResume(extraction)) {
            throw new AiJobDomainException(
                    "RESUME_DOCUMENT_NOT_DETECTED",
                    "This file does not look like a resume/CV. Please upload a resume or CV PDF instead of a book, article, or long document.");
        }
        Language language = languageDetector.resolve(message.language(), resumeText);

        AiProviderClient providerClient = providerRegistry.activeClient();
        AiProviderFeedbackCallResult result = providerClient.generateFeedback(
                promptBuilder.build(message, resumeText, language));
        AiFeedbackResult feedback = result.feedback().orElse(null);
        if (feedback == null) {
            throw new AiJobDomainException(
                    result.errorCode() == null ? "AI_PROVIDER_EMPTY_RESPONSE" : result.errorCode(),
                    "AI provider feedback failure. provider=%s jobId=%s resumeVersionId=%s extractedText=%s detail=%s"
                            .formatted(
                                    providerClient.providerName(),
                                    message.jobId(),
                                    message.resumeVersionId(),
                                    !resumeText.isBlank(),
                                    result.errorDetail()));
        }

        return documentMapper.toDocument(message, feedback);
    }
}
