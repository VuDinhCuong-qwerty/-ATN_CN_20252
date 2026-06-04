package com.iam.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.service-token")
@Data
public class GatewayProperties {
    private String clientId;
    private String clientSecret;
    private String tokenEndpoint;
    private long refreshBufferSeconds = 60;
}
