package com.iam.identity.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.iam.identity.config.KafkaConfig;
import com.iam.identity.kafka.event.BaseEvent;
import com.iam.identity.kafka.event.payload.DefaultPermissionCreatedPayload;
import com.iam.identity.mapper.EventDeserializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultPermissionBackfillConsumer {

    private final EventDeserializer eventDeserializer;
    private final ExecuteEventService executeEventService;

    @KafkaListener(topics = KafkaConfig.TOPIC_DEFAULT_PERMISSION_CREATED, groupId = "iam-group-identity")
    public void handleDefaultPermissionCreated(String message, @Header(KafkaHeaders.OFFSET) long offset, Acknowledgment ack) {
        try {
            BaseEvent<DefaultPermissionCreatedPayload> event = eventDeserializer.deserialize(message,
                    DefaultPermissionCreatedPayload.class);
            log.info("Received event: {} - occurredAt: {} - offset: {}", event.getEventId(), event.getOccurredAt(),
                    offset);

            executeEventService.executeBackfillEvent(event.getPayload());
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize default-permission-created topic offset {}: {}", offset, message, e);
        } catch (Exception e) {
            log.error("Failed to process default-permission-created topic offset {}: {}", offset, message, e);
        } finally {
            ack.acknowledge();
        }
    }

}
