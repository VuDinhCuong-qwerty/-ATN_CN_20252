package com.iam.auth.dto.request;

import lombok.*;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorizeRequest {
    private String clientId;
    private String redirectUri;
    private String responseType;
    private String state;
    private List<String> scopes;
    private String codeChallenge;
    private String codeChallengeMethod;
    private String ssoSession;
    private String nonce;
}
