package com.pedro.resumeapi.ai.kafka;

import com.pedro.common.ai.AiJobRequestedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AiJobEventPublisher {

    private final ObjectProvider<KafkaTemplate<String, AiJobRequestedMessage>> kafkaTemplateProvider;

    @Value("${app.ai-jobs.topic:resume-ai-jobs}")
    private String topic;

    @Value("${app.ai-jobs.kafka-enabled:false}")
    private boolean kafkaEnabled;

    public AiJobEventPublisher(ObjectProvider<KafkaTemplate<String, AiJobRequestedMessage>> kafkaTemplateProvider) {
        this.kafkaTemplateProvider = kafkaTemplateProvider;
    }

    public void publish(AiJobRequestedMessage message) {
        if (!kafkaEnabled) {
            log.debug("Kafka publishing disabled for ai job {}", message.jobId());
            return;
        }

        KafkaTemplate<String, AiJobRequestedMessage> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        if (kafkaTemplate == null) {
            throw new IllegalStateException("AI job Kafka publishing is enabled but no KafkaTemplate is configured");
        }

        String key = message.jobId() == null ? null : message.jobId().toString();
        kafkaTemplate.send(topic, key, message);
    }
}
