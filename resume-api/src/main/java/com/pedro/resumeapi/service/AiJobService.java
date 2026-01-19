package com.pedro.resumeapi.service;

import com.pedro.resumeapi.domain.AiJob;
import com.pedro.resumeapi.domain.ResumeVersion;
import com.pedro.resumeapi.repository.AiJobRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AiJobService {
    private final AiJobRepository aiJobRepository;

    public AiJob createForVersion(ResumeVersion version) {
        AiJob job = new AiJob();
        job.setResumeVersion(version);
        job.setStatus(AiJob.Status.PENDING);
        job.setAttemptCount(0);
        return aiJobRepository.save(job);


    }

}
