package com.iam.auth.kafka.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.auth.config.KafkaConfig;
import com.iam.auth.domain.AuthUserSession;
import com.iam.auth.kafka.event.BaseEvent;
import com.iam.auth.kafka.payload.AppPermissionRevokePayload;
import com.iam.auth.repository.jpa.AuthClientSessionRepository;
import com.iam.auth.repository.jpa.AuthUserSessionRepository;
import com.iam.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionRevokedConsumer {

    private final AuthUserSessionRepository userSessionRepository;
    private final AuthClientSessionRepository clientSessionRepository;
    private final RefreshTokenService refreshTokenService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaConfig.TOPIC_REVOKED_PERMISSION_NOTIFY)
    public void handle(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            BaseEvent<AppPermissionRevokePayload> event = objectMapper.readValue(
                    record.value(),
                    new TypeReference<>() {});

            AppPermissionRevokePayload payload = event.getPayload();
            if (payload == null || payload.getUserId() == null) {
                log.warn("PermissionRevokedConsumer: missing userId, skipping. eventId={}", event.getEventId());
                ack.acknowledge();
                return;
            }

            List<Long> revokedAppIds = payload.getRevokedAppIds();
            if (revokedAppIds == null || revokedAppIds.isEmpty()) {
                log.info("PermissionRevokedConsumer: no revokedAppIds, skipping session/token revocation. userId={}", payload.getUserId());
                ack.acknowledge();
                return;
            }

            Long userId = payload.getUserId();
            log.info("PermissionRevokedConsumer: revoking sessions/tokens for userId={}, appIds={}", userId, revokedAppIds);

            List<AuthUserSession> activeSessions = userSessionRepository.findActiveByUserId(userId);

            for (Long appId : revokedAppIds) {
                for (AuthUserSession session : activeSessions) {
                    clientSessionRepository.invalidateBySessionIdAndAppId(session.getId(), appId);
                }
                refreshTokenService.revokeByUserIdAndAppId(userId, appId);
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("PermissionRevokedConsumer: failed to process record offset={}, partition={}: {}",
                    record.offset(), record.partition(), e.getMessage(), e);
            ack.acknowledge();
        }
    }
}
