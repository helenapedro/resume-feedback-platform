package com.pedro.resumeapi.ai.kafka;

import com.pedro.common.ai.AiJobRequestedMessage;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.config.SslConfigs;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class AiJobKafkaConfig {

    @Bean
    public ProducerFactory<String, AiJobRequestedMessage> aiJobProducerFactory(
            KafkaProperties kafkaProperties
    ) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
        applyHerokuKafkaIfPresent(props);
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

        putIfPresentWithFallback(
                props,
                SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG,
                "SPRING_KAFKA_PROPERTIES_SSL_TRUSTSTORE_CERTIFICATES",
                "KAFKA_TRUSTED_CERT"
        );
        putIfPresentWithFallback(
                props,
                SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG,
                "SPRING_KAFKA_PROPERTIES_SSL_KEYSTORE_CERTIFICATE_CHAIN",
                "KAFKA_CLIENT_CERT"
        );
        putIfPresentWithFallback(
                props,
                SslConfigs.SSL_KEYSTORE_KEY_CONFIG,
                "SPRING_KAFKA_PROPERTIES_SSL_KEYSTORE_KEY",
                "KAFKA_CLIENT_CERT_KEY"
        );
    }

    private void putIfPresentWithFallback(
            Map<String, Object> props,
            String propName,
            String preferredEnvName,
            String fallbackEnvName
    ) {
        String value = System.getenv(preferredEnvName);
        if (value == null || value.isBlank()) {
            value = System.getenv(fallbackEnvName);
        }
        if (value != null && !value.isBlank()) {
            props.put(propName, value);
        }
    }
}
