package com.pedro.resumeapi.ai.service;

import com.pedro.resumeapi.ai.domain.AiJob;
import com.pedro.resumeapi.resume.domain.ResumeVersion;
import com.pedro.resumeapi.ai.repository.AiJobRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AiJobService {
    private final AiJobRepository repo;

    @Transactional
    public AiJob createForVersion(ResumeVersion version) {
        AiJob job = new AiJob();
        job.setResumeVersion(version);
        job.setStatus(AiJob.Status.PENDING);
        job.setAttemptCount(0);

        job.setIdempotencyKey(version.getId().toString());

        return repo.save(job);
    }
}
