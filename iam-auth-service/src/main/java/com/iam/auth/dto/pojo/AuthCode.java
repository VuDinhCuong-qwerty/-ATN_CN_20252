package com.iam.auth.dto.pojo;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthCode {
    private String clientId;
    private String redirectUri;
    private Long userId;
    private List<String> scopes;
    private String codeChallenge;
    private String codeChallengeMethod;
    private LocalDateTime issuerAt;
    private String userSessionId;
    private String nonce;
}
