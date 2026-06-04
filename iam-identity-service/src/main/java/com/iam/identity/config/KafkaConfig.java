package com.iam.identity.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableKafka
@EnableConfigurationProperties(KafkaProperties.class)
@RequiredArgsConstructor
public class KafkaConfig {

    // ── Topic names ───────────────────────────────────────────────────────────

    public static final String TOPIC_CREATE_SUCCESS_USER_NOTIFY = "CREATE-SUCCESS-USER-NOTIFY";
    public static final String TOPIC_DEFAULT_GRANT_PERMISSION_USER = "DEFAULT-GRANT-PERMISSION-USER";
    public static final String TOPIC_USER_CHANGED_PASSWORD = "USER-CHANGED-PASSWORD";
    public static final String TOPIC_REQUEST_PERMISSION_NOTIFY = "REQUEST-PERMISSION-NOTIFY";
    public static final String TOPIC_APPROVE_PERMISSION_NOTIFY = "APPROVE-PERMISSION-NOTIFY";
    public static final String TOPIC_REVOKED_PERMISSION_NOTIFY = "REVOKED-PERMISSION-NOTIFY";

    private final KafkaProperties kafkaProperties;

    // ── Shared SASL/PLAIN base props ──────────────────────────────────────────

    private Map<String, Object> baseProps() {
        String jaasConfig = String.format(
                "org.apache.kafka.common.security.plain.PlainLoginModule required"
                        + " username=\"%s\" password=\"%s\";",
                kafkaProperties.getSasl().getUsername(),
                kafkaProperties.getSasl().getPassword());

        Map<String, Object> props = new HashMap<>();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, jaasConfig);
        return props;
    }

    // ── Producer ──────────────────────────────────────────────────────────────

    @Bean
    ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = baseProps();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ── Consumer ──────────────────────────────────────────────────────────────

    @Bean
    ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = baseProps();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "iam-identity-service");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.iam.*");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL); // manual ack
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
