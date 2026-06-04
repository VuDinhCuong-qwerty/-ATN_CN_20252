package com.iam.identity.kafka.producer;

import java.time.Instant;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.identity.kafka.event.BaseEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdentityEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public <T> void publish(String topic, String eventType, T payload) {
        if (topic == null || topic.isBlank() || eventType == null || eventType.isBlank() || payload == null) {
            return;
        }
        String eventId = UUID.randomUUID().toString();
        BaseEvent<T> event = BaseEvent.<T>builder()
                .eventId(eventId)
                .eventType(eventType)
                .occurredAt(Instant.now())
                .payload(payload)
                .build();

        try {
            String message = objectMapper.writeValueAsString(event); // ✅ convert tại đây

            kafkaTemplate.send(topic, eventId, message) // ✅ gửi String
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event [{}] to topic [{}]: {}",
                                    event.getEventId(), topic, ex.getMessage());
                        } else {
                            log.debug("Published event [{}] to topic [{}]", event.getEventId(), topic);
                        }
                    });

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event [{}]: {}", eventId, e.getMessage(), e);
        }
    }
}
