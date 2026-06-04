package com.iam.app.dto.response;

import com.iam.app.domain.AuthClientMethod;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ClientMethodResponse {

    private final Long id;
    private final Long methodId;
    private final String methodName;
    private final Object config;
    private final String status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ClientMethodResponse(AuthClientMethod entity, String methodName, Object parsedConfig) {
        this.id = entity.getId();
        this.methodId = entity.getMethodId();
        this.methodName = methodName;
        this.config = parsedConfig;
        this.status = entity.getStatus();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
    }
}
