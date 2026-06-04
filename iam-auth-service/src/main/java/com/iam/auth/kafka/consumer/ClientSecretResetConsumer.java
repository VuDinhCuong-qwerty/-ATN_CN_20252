package com.iam.auth.kafka.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.auth.config.KafkaConfig;
import com.iam.auth.kafka.event.BaseEvent;
import com.iam.auth.kafka.payload.ClientSecretResetPayload;
import com.iam.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientSecretResetConsumer {

    private final RefreshTokenService refreshTokenService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaConfig.TOPIC_CLIENT_SECRET_RESET_NOTIFY)
    public void handle(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            BaseEvent<ClientSecretResetPayload> event = objectMapper.readValue(
                    record.value(),
                    new TypeReference<>() {});

            ClientSecretResetPayload payload = event.getPayload();
            if (payload == null || payload.getClientId() == null || payload.getClientId().isBlank()) {
                log.warn("ClientSecretResetConsumer: missing clientId, skipping. eventId={}", event.getEventId());
                ack.acknowledge();
                return;
            }

            String clientId = payload.getClientId();
            log.info("ClientSecretResetConsumer: revoking all refresh tokens for clientId={}, eventType={}",
                    clientId, event.getEventType());

            refreshTokenService.revokeAllByClientId(clientId);

            ack.acknowledge();
        } catch (Exception e) {
            log.error("ClientSecretResetConsumer: failed to process record offset={}, partition={}: {}",
                    record.offset(), record.partition(), e.getMessage(), e);
            ack.acknowledge();
        }
    }
}
