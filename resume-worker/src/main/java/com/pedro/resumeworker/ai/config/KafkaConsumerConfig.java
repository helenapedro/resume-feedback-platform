package com.pedro.resumeworker.ai.config;

import com.pedro.common.ai.AiJobRequestedMessage;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableKafka
@EnableConfigurationProperties(AiJobRetryProperties.class)
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, AiJobRequestedMessage> aiJobConsumerFactory(
            KafkaProperties kafkaProperties
    ) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        applyHerokuKafkaIfPresent(props);

        props.putIfAbsent(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.putIfAbsent(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);

        props.putIfAbsent(JacksonJsonDeserializer.TRUSTED_PACKAGES, "com.pedro.common.ai");
        props.putIfAbsent(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, AiJobRequestedMessage.class.getName());

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JacksonJsonDeserializer<>(AiJobRequestedMessage.class)
        );
    }

    private void applyHerokuKafkaIfPresent(Map<String, Object> props) {
        String kafkaUrl = System.getenv("KAFKA_URL");
        if (kafkaUrl == null || kafkaUrl.isBlank()) {
            return;
        }

        List<String> bootstrapServers = List.of(kafkaUrl.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(URI::create)
                .map(uri -> uri.getHost() + ":" + uri.getPort())
                .collect(Collectors.toList());

        if (!bootstrapServers.isEmpty()) {
            props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.join(",", bootstrapServers));
        }

        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
        props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");
        props.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PEM");

        putIfPresent(props, SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, "KAFKA_TRUSTED_CERT");
        putIfPresent(props, SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG, "KAFKA_CLIENT_CERT");
        putIfPresent(props, SslConfigs.SSL_KEYSTORE_KEY_CONFIG, "KAFKA_CLIENT_CERT_KEY");
    }

    private void putIfPresent(Map<String, Object> props, String propName, String envName) {
        String value = System.getenv(envName);
        if (value != null && !value.isBlank()) {
            props.put(propName, value);
        }
    }
}
