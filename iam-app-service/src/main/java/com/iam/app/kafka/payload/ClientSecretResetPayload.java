package com.iam.app.kafka.payload;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ClientSecretResetPayload {

    private Long clientNumericId;
    private String clientId;
    private String clientType;
    private Long appId;
    private String name;
    private Instant resetAt;
}
