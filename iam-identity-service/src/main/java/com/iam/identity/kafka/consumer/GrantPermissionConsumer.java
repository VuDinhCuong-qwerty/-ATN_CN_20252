package com.iam.identity.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.iam.identity.config.KafkaConfig;
import com.iam.identity.kafka.event.BaseEvent;
import com.iam.identity.kafka.event.payload.UserCreatedPermissionPayload;
import com.iam.identity.mapper.EventDeserializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class GrantPermissionConsumer {

    private final EventDeserializer eventDeserializer;
    private final ExecuteEventService executeEventService;

    @KafkaListener(topics = KafkaConfig.TOPIC_DEFAULT_GRANT_PERMISSION_USER, groupId = "iam-group-identity")
    public void handleGrantPermissionEvent(String message, @Header(KafkaHeaders.OFFSET) long offset, Acknowledgment ack) {
        try {
            BaseEvent<UserCreatedPermissionPayload> event = eventDeserializer.deserialize(message,
                    UserCreatedPermissionPayload.class);
            log.info("Received event: {} - occurredAt: {} - offset: {}", event.getEventId(), event.getOccurredAt(),
                    offset);
            
            executeEventService.executeGrantDefaultEvent(event.getPayload());
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize user-created-topic offset {}: {}", offset, message, e);
        } catch (Exception e) {
            log.error("Failed to process user-created-topic offset {}: {}", offset, message, e);
        } finally {
            ack.acknowledge();
        }
    }

}
