package com.iam.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
