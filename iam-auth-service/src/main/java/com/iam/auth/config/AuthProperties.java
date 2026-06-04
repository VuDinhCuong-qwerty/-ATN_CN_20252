package com.iam.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "auth")
@Component
@Getter
@Setter
public class AuthProperties {

    private String issuer;
    private String baseUrl;
    private Endpoints endpoints = new Endpoints();

    @Getter
    @Setter
    public static class Endpoints {
        private String authorize;
        private String login;
        private String loginPage;
        private String token;
        private String jwks;
        private String userinfo;
        private String logout;
        private String introspect;
        private String revoke;
        private String wellKnown;
    }
}
