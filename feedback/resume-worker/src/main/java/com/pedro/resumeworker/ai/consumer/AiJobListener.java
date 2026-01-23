package com.pedro.resumeworker.ai.consumer;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.resumeworker.ai.service.AiJobProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiJobListener {

    private final AiJobProcessor processor;

    @KafkaListener(
            topics = "${app.ai-jobs.topic:resume-ai-jobs}",
            groupId = "${spring.kafka.consumer.group-id:resume-worker}"
    )
    public void onMessage(AiJobRequestedMessage message) {
        if (message == null || message.jobId() == null) {
            log.warn("Skipping AI job message without job id: {}", message);
            return;
        }
        processor.process(message);
    }
}
