package com.pedro.resumeapi.ai.kafka;

import com.pedro.common.ai.AiJobRequestedMessage;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;


import java.util.HashMap;
import java.util.Map;

@Configuration
public class AiJobKafkaConfig {

    @Bean
    public ProducerFactory<String, AiJobRequestedMessage> aiJobProducerFactory(
            KafkaProperties kafkaProperties
    ) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
        props.putIfAbsent(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.putIfAbsent(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AiJobRequestedMessageSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, AiJobRequestedMessage> aiJobKafkaTemplate(
            ProducerFactory<String, AiJobRequestedMessage> aiJobProducerFactory
    ) {
        return new KafkaTemplate<>(aiJobProducerFactory);
    }
}
