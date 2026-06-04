package com.iam.auth.kafka.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.auth.config.KafkaConfig;
import com.iam.auth.engine.AuthFlowCache;
import com.iam.auth.kafka.event.BaseEvent;
import com.iam.auth.kafka.payload.FlowExecutionUpdatedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlowUpdatedConsumer {

    private final AuthFlowCache authFlowCache;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaConfig.TOPIC_FLOW_EXECUTION_UPDATED)
    public void handle(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            BaseEvent<FlowExecutionUpdatedPayload> event = objectMapper.readValue(
                    record.value(),
                    new TypeReference<>() {});

            FlowExecutionUpdatedPayload payload = event.getPayload();
            if (payload == null || payload.getFlowId() == null) {
                log.warn("FlowUpdatedConsumer: missing flowId, skipping. eventId={}", event.getEventId());
                ack.acknowledge();
                return;
            }

            log.info("FlowUpdatedConsumer: invalidating cache for flowId={}, appId={}", payload.getFlowId(), payload.getAppId());
            authFlowCache.invalidate(payload.getFlowId());

            // Pre-warm cache so the next login request hits DB only once
            if (payload.getAppId() != null) {
                try {
                    authFlowCache.getByAppId(payload.getAppId());
                } catch (Exception ex) {
                    log.warn("FlowUpdatedConsumer: pre-warm failed for appId={}: {}", payload.getAppId(), ex.getMessage());
                }
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("FlowUpdatedConsumer: failed to process record offset={}, partition={}: {}",
                    record.offset(), record.partition(), e.getMessage(), e);
            ack.acknowledge();
        }
    }
}
