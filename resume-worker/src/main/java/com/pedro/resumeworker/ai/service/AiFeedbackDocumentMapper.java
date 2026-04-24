package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.common.ai.mongo.AiFeedbackDocument;
import com.pedro.resumeworker.ai.gemini.GeminiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AiFeedbackDocumentMapper {

    private final String promptVersion;

    public AiFeedbackDocumentMapper(@Value("${app.ai-feedback.prompt-version:v3}") String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public AiFeedbackDocument toDocument(
            AiJobRequestedMessage message,
            GeminiClient.GeminiFeedback feedback,
            String model) {
        AiFeedbackDocument doc = new AiFeedbackDocument();
        doc.setJobId(message.jobId());
        doc.setResumeId(message.resumeId());
        doc.setResumeVersionId(message.resumeVersionId());
        doc.setOwnerId(message.ownerId());
        doc.setCreatedAt(Instant.now());
        doc.setModel(model);
        doc.setPromptVersion(promptVersion);
        doc.setSummary(feedback.summary());
        doc.setStrengths(feedback.strengths());
        doc.setImprovements(feedback.improvements());
        return doc;
    }
}
