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

    public AiFeedbackFactory(
            AiProviderRegistry providerRegistry,
            ResumeTextExtractor resumeTextExtractor,
            AiFeedbackPromptBuilder promptBuilder,
            AiFeedbackDocumentMapper documentMapper) {
        this.providerRegistry = providerRegistry;
        this.resumeTextExtractor = resumeTextExtractor;
        this.promptBuilder = promptBuilder;
        this.documentMapper = documentMapper;
    }

    public AiFeedbackDocument build(AiJobRequestedMessage message, ResumeVersion resumeVersion) {
        String resumeText = resumeTextExtractor.extract(resumeVersion).orElse("");
        if (!StringUtils.hasText(resumeText)) {
            throw new AiJobDomainException(
                    "RESUME_TEXT_NOT_EXTRACTED",
                    "Resume text could not be extracted. jobId=%s resumeVersionId=%s"
                            .formatted(message.jobId(), message.resumeVersionId()));
        }
        Language language = message.language() == null ? Language.EN : message.language();

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
