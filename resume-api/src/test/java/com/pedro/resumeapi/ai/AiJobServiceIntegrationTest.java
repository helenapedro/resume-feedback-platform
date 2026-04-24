package com.pedro.resumeapi.ai;

import com.pedro.common.ai.Language;
import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.resumeapi.ai.domain.AiJob;
import com.pedro.resumeapi.ai.kafka.AiJobEventPublisher;
import com.pedro.resumeapi.ai.repository.AiJobRepository;
import com.pedro.resumeapi.ai.service.AiJobService;
import com.pedro.resumeapi.resume.domain.Resume;
import com.pedro.resumeapi.resume.domain.ResumeVersion;
import com.pedro.resumeapi.resume.repository.ResumeRepository;
import com.pedro.resumeapi.resume.repository.ResumeVersionRepository;
import com.pedro.resumeapi.user.domain.User;
import com.pedro.resumeapi.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@Import(AiJobServiceIntegrationTest.MockPublisherConfig.class)
class AiJobServiceIntegrationTest {

    @jakarta.annotation.Resource
    private AiJobService aiJobService;
    @jakarta.annotation.Resource
    private AiJobRepository aiJobRepository;
    @jakarta.annotation.Resource
    private ResumeRepository resumeRepository;
    @jakarta.annotation.Resource
    private ResumeVersionRepository resumeVersionRepository;
    @jakarta.annotation.Resource
    private UserRepository userRepository;
    @jakarta.annotation.Resource
    private AiJobEventPublisher aiJobEventPublisher;

    private User owner;
    private Resume resume;
    private ResumeVersion version;

    @BeforeEach
    void setUp() {
        aiJobRepository.deleteAll();
        resumeVersionRepository.deleteAll();
        resumeRepository.deleteAll();
        userRepository.deleteAll();
        Mockito.reset(aiJobEventPublisher);

        owner = new User();
        owner.setEmail("owner@example.com");
        owner.setPasswordHash("encoded");
        owner.setRole(User.Role.USER);
        owner.setEnabled(true);
        userRepository.save(owner);

        resume = new Resume();
        resume.setOwner(owner);
        resume.setTitle("Resume");
        resumeRepository.save(resume);

        version = new ResumeVersion();
        version.setResume(resume);
        version.setVersionNumber(1);
        version.setOriginalFilename("resume.pdf");
        version.setFileName("resume.pdf");
        version.setContentType("application/pdf");
        version.setStorageKey("local/path/resume.pdf");
        resumeVersionRepository.save(version);
    }

    @Test
    void createForVersionPublishesKafkaMessageAfterCommit() {
        AiJob job = aiJobService.createForVersion(version, "version-1", Language.PT);

        assertNotNull(job.getId());
        assertEquals(1, aiJobRepository.count());

        ArgumentCaptor<AiJobRequestedMessage> captor = ArgumentCaptor.forClass(AiJobRequestedMessage.class);
        verify(aiJobEventPublisher, times(1)).publish(captor.capture());

        AiJobRequestedMessage message = captor.getValue();
        assertEquals(job.getId(), message.jobId());
        assertEquals(resume.getId(), message.resumeId());
        assertEquals(version.getId(), message.resumeVersionId());
        assertEquals(owner.getId(), message.ownerId());
        assertEquals(Language.PT, message.language());
    }

    @TestConfiguration
    static class MockPublisherConfig {
        @Bean
        @Primary
        AiJobEventPublisher aiJobEventPublisher() {
            return Mockito.mock(AiJobEventPublisher.class);
        }
    }
}
