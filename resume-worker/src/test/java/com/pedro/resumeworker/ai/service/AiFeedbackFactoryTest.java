package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.common.ai.Language;
import com.pedro.common.ai.mongo.AiFeedbackDocument;
import com.pedro.resumeworker.ai.domain.ResumeVersion;
import com.pedro.resumeworker.ai.gemini.GeminiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiFeedbackFactoryTest {

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private ResumeTextExtractor resumeTextExtractor;

    private AiFeedbackFactory factory;

    @BeforeEach
    void setUp() {
        factory = new AiFeedbackFactory("v2", 12000, geminiClient, resumeTextExtractor);
    }

    @Test
    void buildUsesRecruiterStylePromptAndReturnsFeedbackDocument() {
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

        when(resumeTextExtractor.extract(version)).thenReturn(Optional.of("Built Kafka pipelines and Spring Boot services."));
        when(geminiClient.generateFeedbackWithDiagnostics(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new GeminiClient.GeminiCallResult(
                        Optional.of(new GeminiClient.GeminiFeedback(
                                "Strong mid-level resume with clear ownership; needs sharper senior-level signaling.",
                                List.of("Experience: Strong impact and ownership evidence."),
                                List.of("Projects: Add clearer system design tradeoffs.")
                        )),
                        null,
                        null));
        when(geminiClient.effectiveModel()).thenReturn("gemini-test");

        AiFeedbackDocument document = factory.build(message, version);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiClient).generateFeedbackWithDiagnostics(promptCaptor.capture());
        String prompt = promptCaptor.getValue();

        assertTrue(prompt.contains("technical recruiter or hiring manager"));
        assertTrue(prompt.contains("how competitive the resume is now"));
        assertTrue(prompt.contains("system design signals"));
        assertTrue(prompt.contains("Experience:"));

        assertEquals(jobId, document.getJobId());
        assertEquals(resumeId, document.getResumeId());
        assertEquals(resumeVersionId, document.getResumeVersionId());
        assertEquals(ownerId, document.getOwnerId());
        assertEquals("gemini-test", document.getModel());
        assertEquals("v2", document.getPromptVersion());
        assertEquals("Strong mid-level resume with clear ownership; needs sharper senior-level signaling.",
                document.getSummary());
        assertEquals(List.of("Experience: Strong impact and ownership evidence."), document.getStrengths());
        assertEquals(List.of("Projects: Add clearer system design tradeoffs."), document.getImprovements());
    }
}
