package com.pedro.resumeapi.ai.kafka;

import com.pedro.common.ai.AiJobRequestedMessage;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AiJobEventPublisher {

    private final KafkaTemplate<String, AiJobRequestedMessage> kafkaTemplate;

    @Value("${app.ai-jobs.topic:resume-ai-jobs}")
    private String topic;

    public void publish(AiJobRequestedMessage message) {
        String key = message.jobId() == null ? null : message.jobId().toString();
        kafkaTemplate.send(topic, key, message);
    }
}
