package com.iam.auth.kafka.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.auth.config.KafkaConfig;
import com.iam.auth.kafka.event.BaseEvent;
import com.iam.auth.kafka.payload.UserPasswordChangedPayload;
import com.iam.auth.service.RefreshTokenService;
import com.iam.auth.service.SSOService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordChangedConsumer {

    private final SSOService ssoService;
    private final RefreshTokenService refreshTokenService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaConfig.TOPIC_USER_CHANGED_PASSWORD)
    public void handle(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            BaseEvent<UserPasswordChangedPayload> event = objectMapper.readValue(
                    record.value(),
                    new TypeReference<>() {});

            UserPasswordChangedPayload payload = event.getPayload();
            if (payload == null || payload.getUserId() == null) {
                log.warn("PasswordChangedConsumer: missing userId, skipping. eventId={}", event.getEventId());
                ack.acknowledge();
                return;
            }

            Long userId = payload.getUserId();
            log.info("PasswordChangedConsumer: revoking sessions for userId={}, eventType={}", userId, event.getEventType());

            ssoService.revokeAllSessionsByUserId(userId);
            refreshTokenService.revokeAllByUserId(userId);

            ack.acknowledge();
        } catch (Exception e) {
            log.error("PasswordChangedConsumer: failed to process record offset={}, partition={}: {}",
                    record.offset(), record.partition(), e.getMessage(), e);
            ack.acknowledge();
        }
    }
}
