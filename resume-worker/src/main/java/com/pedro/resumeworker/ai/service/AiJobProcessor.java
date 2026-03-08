package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.resumeworker.ai.config.AiJobRetryProperties;
import com.pedro.resumeworker.ai.domain.AiFeedbackRef;
import com.pedro.resumeworker.ai.domain.AiJob;
import com.pedro.resumeworker.ai.domain.AiProgressRef;
import com.pedro.resumeworker.ai.domain.ResumeVersion;
import com.pedro.common.ai.mongo.AiFeedbackDocument;
import com.pedro.common.ai.mongo.AiProgressDocument;
import com.pedro.resumeworker.ai.mongo.AiFeedbackMongoRepository;
import com.pedro.resumeworker.ai.mongo.AiProgressMongoRepository;
import com.pedro.resumeworker.ai.repository.AiFeedbackRefRepository;
import com.pedro.resumeworker.ai.repository.AiJobRepository;
import com.pedro.resumeworker.ai.repository.AiProgressRefRepository;
import com.pedro.resumeworker.ai.repository.ResumeVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiJobProcessor {

    private final AiJobRepository aiJobRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final AiFeedbackRefRepository aiFeedbackRefRepository;
    private final AiFeedbackMongoRepository feedbackMongoRepository;
    private final AiProgressRefRepository aiProgressRefRepository;
    private final AiProgressMongoRepository progressMongoRepository;
    private final AiFeedbackFactory feedbackFactory;
    private final AiProgressFactory progressFactory;
    private final AiJobRetryProperties retryProperties;

    public void process(AiJobRequestedMessage message) {
        if (message.resumeVersionId() == null) {
            log.warn("AI job message missing resume version id: {}", message);
            return;
        }
        Optional<AiJob> jobOpt = aiJobRepository.findById(message.jobId());
        if (jobOpt.isEmpty()) {
            log.warn("AI job not found in MySQL: {}", message.jobId());
            return;
        }

        AiJob job = jobOpt.get();
        if (job.getStatus() == AiJob.Status.DONE) {
            log.info("AI job already completed: {}", job.getId());
            return;
        }
        if (job.getStatus() == AiJob.Status.FAILED) {
            if (job.getAttemptCount() >= retryProperties.maxAttempts()) {
                log.warn("AI job exceeded retry attempts: {}", job.getId());
                return;
            }
            if (job.getNextRetryAt() != null && Instant.now().isBefore(job.getNextRetryAt())) {
                log.info("AI job retry not due yet: {}", job.getId());
                return;
            }
        }

        try {
            ResumeVersion resumeVersion = resumeVersionRepository.findById(message.resumeVersionId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Resume version not found: " + message.resumeVersionId()));

            markProcessing(job);
            AiFeedbackDocument document = feedbackFactory.build(message, resumeVersion);
            AiFeedbackDocument savedDoc = feedbackMongoRepository.save(document);
            int nextVersion = nextFeedbackVersion(message.resumeVersionId());

            AiFeedbackRef ref = new AiFeedbackRef();
            ref.setResumeVersion(resumeVersion);
            ref.setFeedbackVersion(nextVersion);
            ref.setMongoDocId(savedDoc.getId());
            ref.setModel(savedDoc.getModel());
            ref.setPromptVersion(savedDoc.getPromptVersion());
            aiFeedbackRefRepository.save(ref);

            try {
                maybeCreateProgress(message, resumeVersion);
            } catch (Exception progressEx) {
                log.warn("AI progress generation skipped for resumeVersionId={}: {}",
                        resumeVersion.getId(),
                        progressEx.getMessage());
            }

            markDone(job);
        } catch (Exception ex) {
            markFailed(job, ex);
        }
    }

    void markProcessing(AiJob job) {
        job.setStatus(AiJob.Status.PROCESSING);
        job.setAttemptCount(job.getAttemptCount() + 1);
        if (job.getStartedAt() == null) {
            job.setStartedAt(Instant.now());
        }
        job.setErrorCode(null);
        job.setErrorDetail(null);
        job.setNextRetryAt(null);
        aiJobRepository.save(job);
    }

    void markDone(AiJob job) {
        job.setStatus(AiJob.Status.DONE);
        job.setFinishedAt(Instant.now());
        job.setErrorCode(null);
        job.setErrorDetail(null);
        job.setNextRetryAt(null);
        aiJobRepository.save(job);
    }

    void markFailed(AiJob job, Exception ex) {
        job.setStatus(AiJob.Status.FAILED);
        job.setFinishedAt(Instant.now());
        if (ex instanceof AiJobDomainException domainEx) {
            job.setErrorCode(domainEx.getErrorCode());
        } else {
            job.setErrorCode(ex.getClass().getSimpleName());
        }
        job.setErrorDetail(truncateErrorDetail(ex.getMessage()));
        job.setNextRetryAt(calculateNextRetryAt(job));
        aiJobRepository.save(job);
        log.error("AI job failed: {}", job.getId(), ex);
    }

    private int nextFeedbackVersion(UUID resumeVersionId) {
        return aiFeedbackRefRepository.findTopByResumeVersion_IdOrderByFeedbackVersionDesc(resumeVersionId)
                .map(ref -> ref.getFeedbackVersion() + 1)
                .orElse(1);
    }

    private Instant calculateNextRetryAt(AiJob job) {
        if (job.getAttemptCount() >= retryProperties.maxAttempts()) {
            return null;
        }
        long exponent = Math.max(0, job.getAttemptCount() - 1);
        long baseMillis = retryProperties.initialBackoff().toMillis();
        long maxMillis = retryProperties.maxBackoff().toMillis();
        long delayMillis = Math.min(maxMillis, baseMillis * (1L << exponent));
        return Instant.now().plus(Duration.ofMillis(delayMillis));
    }

    private String truncateErrorDetail(String message) {
        if (message == null) {
            return null;
        }
        int max = 1024;
        return message.length() > max ? message.substring(0, max) : message;
    }

    private void maybeCreateProgress(AiJobRequestedMessage message, ResumeVersion currentVersion) {
        if (currentVersion.getResumeId() == null || currentVersion.getVersionNumber() == null) {
            return;
        }

        Optional<ResumeVersion> previousVersionOpt = resumeVersionRepository
                .findTopByResumeIdAndVersionNumberLessThanOrderByVersionNumberDesc(
                        currentVersion.getResumeId(),
                        currentVersion.getVersionNumber());
        if (previousVersionOpt.isEmpty()) {
            return;
        }

        ResumeVersion previousVersion = previousVersionOpt.get();
        Optional<AiFeedbackDocument> previousFeedbackOpt = feedbackMongoRepository
                .findTopByResumeVersionIdOrderByCreatedAtDesc(previousVersion.getId());
        if (previousFeedbackOpt.isEmpty()) {
            log.info("Skipping AI progress for resumeVersionId={} because baseline feedback is missing",
                    currentVersion.getId());
            return;
        }

        AiProgressDocument progressDoc = progressFactory.build(
                message,
                currentVersion,
                previousVersion,
                previousFeedbackOpt.get());
        AiProgressDocument savedProgressDoc = progressMongoRepository.save(progressDoc);
        int nextProgressVersion = nextProgressVersion(currentVersion.getId());

        AiProgressRef progressRef = new AiProgressRef();
        progressRef.setResumeVersion(currentVersion);
        progressRef.setBaselineResumeVersion(previousVersion);
        progressRef.setProgressVersion(nextProgressVersion);
        progressRef.setMongoDocId(savedProgressDoc.getId());
        progressRef.setModel(savedProgressDoc.getModel());
        progressRef.setPromptVersion(savedProgressDoc.getPromptVersion());
        aiProgressRefRepository.save(progressRef);
    }

    private int nextProgressVersion(UUID resumeVersionId) {
        return aiProgressRefRepository.findTopByResumeVersion_IdOrderByProgressVersionDesc(resumeVersionId)
                .map(ref -> ref.getProgressVersion() + 1)
                .orElse(1);
    }
}
