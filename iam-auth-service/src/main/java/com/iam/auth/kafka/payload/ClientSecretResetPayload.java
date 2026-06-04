package com.iam.auth.kafka.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientSecretResetPayload {
    private Long clientNumericId;
    private String clientId;
    private String clientType;
    private Long appId;
    private String name;
    private Instant resetAt;
}
