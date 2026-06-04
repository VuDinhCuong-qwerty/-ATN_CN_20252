package com.iam.auth.engine;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthRequest {
    private String authRequestId;
    private Long clientId;
    private String redirectUri;
    private String state;
    private String scope;
    private String codeChallenge;
    private String codeChallengeMethod;
}
