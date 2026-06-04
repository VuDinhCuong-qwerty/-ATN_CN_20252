package com.iam.identity.mapper;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.identity.kafka.event.BaseEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EventDeserializer {

    private final ObjectMapper objectMapper;

    public <T> BaseEvent<T> deserialize(String message, Class<T> payloadClass) throws JsonProcessingException {
        JavaType type = objectMapper.getTypeFactory()
                .constructParametricType(BaseEvent.class, payloadClass);
        return objectMapper.readValue(message, type);
    }
}
