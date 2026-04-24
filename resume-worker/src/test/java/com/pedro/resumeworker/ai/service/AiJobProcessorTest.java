package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.common.ai.Language;
import com.pedro.common.ai.mongo.AiFeedbackDocument;
import com.pedro.common.ai.mongo.AiProgressDocument;
import com.pedro.resumeworker.ai.config.AiJobRetryProperties;
import com.pedro.resumeworker.ai.domain.AiJob;
import com.pedro.resumeworker.ai.domain.ResumeVersion;
import com.pedro.resumeworker.ai.mongo.AiFeedbackMongoRepository;
import com.pedro.resumeworker.ai.mongo.AiProgressMongoRepository;
import com.pedro.resumeworker.ai.repository.AiFeedbackRefRepository;
import com.pedro.resumeworker.ai.repository.AiJobRepository;
import com.pedro.resumeworker.ai.repository.AiProgressRefRepository;
import com.pedro.resumeworker.ai.repository.ResumeVersionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiJobProcessorTest {

    @Mock
    private AiJobRepository aiJobRepository;
    @Mock
    private ResumeVersionRepository resumeVersionRepository;
    @Mock
    private AiFeedbackRefRepository aiFeedbackRefRepository;
    @Mock
    private AiFeedbackMongoRepository feedbackMongoRepository;
    @Mock
    private AiProgressRefRepository aiProgressRefRepository;
    @Mock
    private AiProgressMongoRepository progressMongoRepository;
    @Mock
    private AiFeedbackFactory feedbackFactory;
    @Mock
    private AiProgressFactory progressFactory;

    private AiJobProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new AiJobProcessor(
                aiJobRepository,
                resumeVersionRepository,
                aiFeedbackRefRepository,
                feedbackMongoRepository,
                aiProgressRefRepository,
                progressMongoRepository,
                feedbackFactory,
                progressFactory,
                new AiJobRetryProperties(5, Duration.ofSeconds(10), Duration.ofMinutes(5)));
    }

    @Test
    void processSuccessfulMessageStoresFeedbackAndProgressAndMarksDone() {
        UUID jobId = UUID.randomUUID();
        UUID resumeId = UUID.randomUUID();
        UUID currentVersionId = UUID.randomUUID();
        UUID previousVersionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        AiJobRequestedMessage message = new AiJobRequestedMessage(
                jobId,
                resumeId,
                currentVersionId,
                ownerId,
                Instant.now(),
                Language.EN);

        AiJob job = new AiJob();
        job.setId(jobId);
        job.setStatus(AiJob.Status.PENDING);

        ResumeVersion currentVersion = new ResumeVersion();
        currentVersion.setId(currentVersionId);
        currentVersion.setResumeId(resumeId);
        currentVersion.setVersionNumber(2);

        ResumeVersion previousVersion = new ResumeVersion();
        previousVersion.setId(previousVersionId);
        previousVersion.setResumeId(resumeId);
        previousVersion.setVersionNumber(1);

        AiFeedbackDocument feedbackDoc = new AiFeedbackDocument();
        feedbackDoc.setId("feedback-doc-1");
        feedbackDoc.setModel("gemini-test");
        feedbackDoc.setPromptVersion("v1");

        AiFeedbackDocument baselineFeedback = new AiFeedbackDocument();
        baselineFeedback.setId("baseline-doc");

        AiProgressDocument progressDoc = new AiProgressDocument();
        progressDoc.setId("progress-doc-1");
        progressDoc.setModel("gemini-test");
        progressDoc.setPromptVersion("v1");

        when(aiJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(resumeVersionRepository.findById(currentVersionId)).thenReturn(Optional.of(currentVersion));
        when(feedbackFactory.build(message, currentVersion)).thenReturn(feedbackDoc);
        when(feedbackMongoRepository.save(feedbackDoc)).thenReturn(feedbackDoc);
        when(aiFeedbackRefRepository.findTopByResumeVersion_IdOrderByFeedbackVersionDesc(currentVersionId))
                .thenReturn(Optional.empty());
        when(resumeVersionRepository.findTopByResumeIdAndVersionNumberLessThanOrderByVersionNumberDesc(resumeId, 2))
                .thenReturn(Optional.of(previousVersion));
        when(feedbackMongoRepository.findTopByResumeVersionIdOrderByCreatedAtDesc(previousVersionId))
                .thenReturn(Optional.of(baselineFeedback));
        when(progressFactory.build(message, currentVersion, previousVersion, baselineFeedback)).thenReturn(progressDoc);
        when(progressMongoRepository.save(progressDoc)).thenReturn(progressDoc);
        when(aiProgressRefRepository.findTopByResumeVersion_IdOrderByProgressVersionDesc(currentVersionId))
                .thenReturn(Optional.empty());

        processor.process(message);

        assertEquals(AiJob.Status.DONE, job.getStatus());
        assertEquals(1, job.getAttemptCount());
        assertNotNull(job.getStartedAt());
        assertNotNull(job.getFinishedAt());

        verify(aiFeedbackRefRepository).save(any());
        verify(aiProgressRefRepository).save(any());
        verify(aiJobRepository, atLeast(2)).save(job);
    }

    @Test
    void processFailureMarksJobFailedAndSchedulesRetry() {
        UUID jobId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();

        AiJobRequestedMessage message = new AiJobRequestedMessage(
                jobId,
                UUID.randomUUID(),
                versionId,
                UUID.randomUUID(),
                Instant.now(),
                Language.EN);

        AiJob job = new AiJob();
        job.setId(jobId);
        job.setStatus(AiJob.Status.PENDING);

        ResumeVersion currentVersion = new ResumeVersion();
        currentVersion.setId(versionId);
        currentVersion.setResumeId(UUID.randomUUID());
        currentVersion.setVersionNumber(1);

        when(aiJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(resumeVersionRepository.findById(versionId)).thenReturn(Optional.of(currentVersion));
        when(feedbackFactory.build(message, currentVersion))
                .thenThrow(new AiJobDomainException("PROMPT_INVALID", "Prompt assembly failed"));

        processor.process(message);

        assertEquals(AiJob.Status.FAILED, job.getStatus());
        assertEquals(1, job.getAttemptCount());
        assertEquals("PROMPT_INVALID", job.getErrorCode());
        assertEquals("Prompt assembly failed", job.getErrorDetail());
        assertNotNull(job.getNextRetryAt());

        verify(aiFeedbackRefRepository, never()).save(any());
        verify(aiProgressRefRepository, never()).save(any());
        verify(aiJobRepository, atLeast(2)).save(job);
    }
}
