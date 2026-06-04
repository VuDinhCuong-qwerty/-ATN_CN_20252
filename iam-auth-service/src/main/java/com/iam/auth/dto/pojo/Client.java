package com.iam.auth.dto.pojo;

import lombok.*;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {
    private Long id;
    private String clientId;
    private String clientSecret;
    private String name;
    private String clientType;
    private boolean enabled;
    private Long appId;
    private List<String> grantTypes;
    private List<String> redirectUris;
    private List<String> allowedScopes;
    private Long accessTokenTTL;
    private Long refreshTokenTTL;
    private Long idTokenTTL;
    private List<String> tokenEndpointAuth;
    private boolean requiredPKCE;
    private boolean requiredConsent;
    private String postLogoutRedirect;
    private String defaultUrl;
}
