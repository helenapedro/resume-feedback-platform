package com.pedro.resumeworker.ai.service;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.resumeworker.ai.config.AiJobRetryProperties;
import com.pedro.resumeworker.ai.domain.AiJob;
import com.pedro.resumeworker.ai.domain.ResumeVersion;
import com.pedro.resumeworker.ai.repository.AiJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiJobRetryScheduler {

    private final AiJobRepository aiJobRepository;
    private final AiJobProcessor processor;
    private final AiJobRetryProperties retryProperties;

    @Scheduled(fixedDelayString = "${app.ai-jobs.retry.poll-interval:PT30S}")
    public void retryFailedJobs() {
        List<AiJob> dueJobs = aiJobRepository.findByStatusAndNextRetryAtBefore(
                AiJob.Status.FAILED,
                Instant.now()
        );

        for (AiJob job : dueJobs) {
            if (job.getAttemptCount() >= retryProperties.maxAttempts()) {
                continue;
            }
            ResumeVersion version = job.getResumeVersion();
            AiJobRequestedMessage message = new AiJobRequestedMessage(
                    job.getId(),
                    version.getResumeId(),
                    version.getId(),
                    version.getCreatedBy(),
                    job.getCreatedAt()
            );
            processor.process(message);
        }
    }
}
