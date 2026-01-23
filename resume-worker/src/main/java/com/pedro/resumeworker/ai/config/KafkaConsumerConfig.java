package com.pedro.resumeworker.ai.config;

import com.pedro.common.ai.AiJobRequestedMessage;
import com.pedro.resumeworker.ai.config.AiJobRetryProperties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@EnableConfigurationProperties(AiJobRetryProperties.class)
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, AiJobRequestedMessage> aiJobConsumerFactory(
            KafkaProperties kafkaProperties
    ) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        props.putIfAbsent(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.putIfAbsent(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.putIfAbsent(JsonDeserializer.TRUSTED_PACKAGES, "com.pedro.common.ai");
        props.putIfAbsent(JsonDeserializer.VALUE_DEFAULT_TYPE, AiJobRequestedMessage.class.getName());
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(AiJobRequestedMessage.class, false)
        );
    }
}
