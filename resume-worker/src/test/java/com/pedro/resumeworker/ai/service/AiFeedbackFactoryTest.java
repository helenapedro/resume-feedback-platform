package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.common.ai.Language;
import com.pedro.common.ai.mongo.AiFeedbackDocument;
import com.pedro.resumeworker.ai.domain.ResumeVersion;
import com.pedro.resumeworker.ai.provider.AiFeedbackResult;
import com.pedro.resumeworker.ai.provider.AiProviderClient;
import com.pedro.resumeworker.ai.provider.AiProviderFeedbackCallResult;
import com.pedro.resumeworker.ai.provider.AiProviderRegistry;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiFeedbackFactoryTest {

    @Mock
    private AiProviderRegistry providerRegistry;

    @Mock
    private AiProviderClient providerClient;

    @Mock
    private ResumeTextExtractor resumeTextExtractor;

    @Mock
    private AiFeedbackPromptBuilder promptBuilder;

    @Mock
    private AiFeedbackDocumentMapper documentMapper;
    @Mock
    private ResumeLanguageDetector languageDetector;
    @Mock
    private ResumeDocumentClassifier documentClassifier;

    private AiFeedbackFactory factory;

    @BeforeEach
    void setUp() {
        factory = new AiFeedbackFactory(
                providerRegistry,
                resumeTextExtractor,
                promptBuilder,
                documentMapper,
                languageDetector,
                documentClassifier);
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
        AiFeedbackResult feedback = new AiFeedbackResult(
                "Strong mid-level resume with clear ownership; needs sharper senior-level signaling.",
                List.of("Experience: Strong impact and ownership evidence."),
                List.of("Projects: Add clearer system design tradeoffs."),
                "gemini:gemini-test");
        AiProviderFeedbackCallResult result = new AiProviderFeedbackCallResult(Optional.of(feedback), null, null);

        AiFeedbackDocument mappedDocument = new AiFeedbackDocument();
        mappedDocument.setPromptVersion("v3");
        mappedDocument.setModel("gemini:gemini-test");

        ResumeTextExtractor.ResumeTextExtraction extraction =
                new ResumeTextExtractor.ResumeTextExtraction(resumeText, 2);
        when(resumeTextExtractor.extractWithMetadata(version)).thenReturn(Optional.of(extraction));
        when(documentClassifier.isLikelyResume(extraction)).thenReturn(true);
        when(languageDetector.resolve(Language.EN, resumeText)).thenReturn(Language.EN);
        when(promptBuilder.build(message, resumeText, Language.EN)).thenReturn(prompt);
        when(providerRegistry.activeClient()).thenReturn(providerClient);
        when(providerClient.generateFeedback(prompt)).thenReturn(result);
        when(documentMapper.toDocument(message, feedback)).thenReturn(mappedDocument);

        AiFeedbackDocument document = factory.build(message, version);

        verify(promptBuilder).build(message, resumeText, Language.EN);
        verify(providerClient).generateFeedback(prompt);
        verify(documentMapper).toDocument(message, feedback);
        assertEquals(mappedDocument, document);
    }

    @Test
    void buildFailsBeforeProviderCallWhenResumeTextCannotBeExtracted() {
        AiJobRequestedMessage message = new AiJobRequestedMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                Language.EN);
        ResumeVersion version = new ResumeVersion();
        version.setId(message.resumeVersionId());

        when(resumeTextExtractor.extractWithMetadata(version)).thenReturn(Optional.empty());

        AiJobDomainException ex = assertThrows(AiJobDomainException.class, () -> factory.build(message, version));

        assertEquals("RESUME_TEXT_NOT_EXTRACTED", ex.getErrorCode());
        assertFalse(ex.getMessage().contains("jobId"));
        verify(promptBuilder, never()).build(any(), anyString(), any());
        verify(providerRegistry, never()).activeClient();
    }

    @Test
    void buildFailsBeforeProviderCallWhenDocumentDoesNotLookLikeResume() {
        AiJobRequestedMessage message = new AiJobRequestedMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                Language.AUTO);
        ResumeVersion version = new ResumeVersion();
        version.setId(message.resumeVersionId());

        ResumeTextExtractor.ResumeTextExtraction extraction =
                new ResumeTextExtractor.ResumeTextExtraction("chapter one long book text", 153);
        when(resumeTextExtractor.extractWithMetadata(version)).thenReturn(Optional.of(extraction));
        when(documentClassifier.isLikelyResume(extraction)).thenReturn(false);

        AiJobDomainException ex = assertThrows(AiJobDomainException.class, () -> factory.build(message, version));

        assertEquals("RESUME_DOCUMENT_NOT_DETECTED", ex.getErrorCode());
        assertEquals(
                "This file does not look like a resume/CV. Please upload a resume or CV PDF instead of a book, article, or long document.",
                ex.getMessage());
        verify(promptBuilder, never()).build(any(), anyString(), any());
        verify(providerRegistry, never()).activeClient();
    }
}
