package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.common.ai.Language;
import com.pedro.common.ai.mongo.AiFeedbackDocument;
import com.pedro.resumeworker.ai.domain.ResumeVersion;
import com.pedro.resumeworker.ai.gemini.GeminiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiFeedbackFactoryTest {

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private ResumeTextExtractor resumeTextExtractor;

    @Mock
    private AiFeedbackPromptBuilder promptBuilder;

    @Mock
    private AiFeedbackDocumentMapper documentMapper;

    private AiFeedbackFactory factory;

    @BeforeEach
    void setUp() {
        factory = new AiFeedbackFactory(geminiClient, resumeTextExtractor, promptBuilder, documentMapper);
    }

    @Test
    void buildDelegatesPromptingProviderCallAndDocumentMapping() {
        UUID jobId = UUID.randomUUID();
        UUID resumeId = UUID.randomUUID();
        UUID resumeVersionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        AiJobRequestedMessage message = new AiJobRequestedMessage(
                jobId,
                resumeId,
                resumeVersionId,
                ownerId,
                Instant.now(),
                Language.EN);

        ResumeVersion version = new ResumeVersion();
        version.setId(resumeVersionId);

        String resumeText = "Built Kafka pipelines and Spring Boot services.";
        String prompt = "prompt";
        GeminiClient.GeminiFeedback feedback = new GeminiClient.GeminiFeedback(
                "Strong mid-level resume with clear ownership; needs sharper senior-level signaling.",
                List.of("Experience: Strong impact and ownership evidence."),
                List.of("Projects: Add clearer system design tradeoffs."));
        GeminiClient.GeminiCallResult result = new GeminiClient.GeminiCallResult(Optional.of(feedback), null, null);

        AiFeedbackDocument mappedDocument = new AiFeedbackDocument();
        mappedDocument.setPromptVersion("v3");
        mappedDocument.setModel("gemini-test");

        when(resumeTextExtractor.extract(version)).thenReturn(Optional.of(resumeText));
        when(promptBuilder.build(message, resumeText, Language.EN)).thenReturn(prompt);
        when(geminiClient.generateFeedbackWithDiagnostics(prompt)).thenReturn(result);
        when(geminiClient.effectiveModel()).thenReturn("gemini-test");
        when(documentMapper.toDocument(message, feedback, "gemini-test")).thenReturn(mappedDocument);

        AiFeedbackDocument document = factory.build(message, version);

        verify(promptBuilder).build(message, resumeText, Language.EN);
        verify(geminiClient).generateFeedbackWithDiagnostics(prompt);
        verify(documentMapper).toDocument(message, feedback, "gemini-test");
        assertEquals(mappedDocument, document);
    }
}
