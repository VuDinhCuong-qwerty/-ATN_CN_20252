package com.iam.identity.kafka.event;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaseEvent<T> {
    private String eventId;
    private String eventType;
    private Instant occurredAt;
    private T payload;
}
