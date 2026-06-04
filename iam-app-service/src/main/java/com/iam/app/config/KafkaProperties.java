package com.iam.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "kafka")
public class KafkaProperties {

    private String bootstrapServers;
    private Sasl sasl = new Sasl();

    @Getter
    @Setter
    public static class Sasl {
        private String username;
        private String password;
    }
}
