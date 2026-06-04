package com.iam.auth.service;

import com.iam.auth.domain.AuthRefreshToken;

import java.util.List;

public interface RefreshTokenService {
    String createRefreshToken(Long userId, String clientId, Long appId, List<String> scopes, String userSessionId, long ttlSeconds);
    AuthRefreshToken getRefreshToken(String token);
    void revokeRefreshToken(String token);

    void revokeBySession(String sessionId, String clientId);

    void revokeAllByUserId(Long userId);

    void revokeByUserIdAndAppId(Long userId, Long appId);

    void revokeAllByClientId(String clientId);
}
