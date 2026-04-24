package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.common.ai.Language;
import com.pedro.common.ai.mongo.AiFeedbackDocument;
import com.pedro.resumeworker.ai.domain.ResumeVersion;
import com.pedro.resumeworker.ai.gemini.GeminiClient;
import org.springframework.stereotype.Component;

@Component
public class AiFeedbackFactory {

    private final GeminiClient geminiClient;
    private final ResumeTextExtractor resumeTextExtractor;
    private final AiFeedbackPromptBuilder promptBuilder;
    private final AiFeedbackDocumentMapper documentMapper;

    public AiFeedbackFactory(
            GeminiClient geminiClient,
            ResumeTextExtractor resumeTextExtractor,
            AiFeedbackPromptBuilder promptBuilder,
            AiFeedbackDocumentMapper documentMapper) {
        this.geminiClient = geminiClient;
        this.resumeTextExtractor = resumeTextExtractor;
        this.promptBuilder = promptBuilder;
        this.documentMapper = documentMapper;
    }

    public AiFeedbackDocument build(AiJobRequestedMessage message, ResumeVersion resumeVersion) {
        String resumeText = resumeTextExtractor.extract(resumeVersion).orElse("");
        Language language = message.language() == null ? Language.EN : message.language();

        GeminiClient.GeminiCallResult result = geminiClient.generateFeedbackWithDiagnostics(
                promptBuilder.build(message, resumeText, language));
        GeminiClient.GeminiFeedback feedback = result.feedback().orElse(null);
        if (feedback == null) {
            throw new AiJobDomainException(
                    result.errorCode() == null ? "AI_PROVIDER_EMPTY_RESPONSE" : result.errorCode(),
                    "Gemini feedback failure. jobId=%s resumeVersionId=%s extractedText=%s detail=%s"
                            .formatted(
                                    message.jobId(),
                                    message.resumeVersionId(),
                                    !resumeText.isBlank(),
                                    result.errorDetail()));
        }

        return documentMapper.toDocument(message, feedback, geminiClient.effectiveModel());
    }
}
