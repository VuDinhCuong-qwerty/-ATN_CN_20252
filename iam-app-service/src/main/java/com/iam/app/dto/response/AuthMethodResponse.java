package com.iam.app.dto.response;

import java.time.LocalDateTime;

import com.iam.app.domain.AuthMethod;

import lombok.Getter;

@Getter
public class AuthMethodResponse {

    private final Long id;
    private final String method;
    private final Integer status;
    private final LocalDateTime createdAt;

    public AuthMethodResponse(AuthMethod entity) {
        this.id = entity.getId();
        this.method = entity.getMethod();
        this.status = entity.getStatus();
        this.createdAt = entity.getCreatedAt();
    }
}
