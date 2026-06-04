package com.iam.auth.service.impl;

import com.iam.auth.domain.AuthRefreshToken;
import com.iam.auth.repository.jpa.AuthRefreshTokenRepository;
import com.iam.auth.service.BaseService;
import com.iam.auth.service.RefreshTokenService;
import com.iam.auth.utils.Utility;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl extends BaseService implements RefreshTokenService {

    private final AuthRefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public String createRefreshToken(Long userId, String clientId, Long appId,
                                     List<String> scopes, String userSessionId, long ttlSeconds) {
        String token = Utility.generateAuthorizationCode();

        AuthRefreshToken refreshToken = AuthRefreshToken.builder()
                .token(token)
                .userId(userId)
                .clientId(clientId)
                .appId(appId)
                .scopes(String.join(",", scopes))
                .userSessionId(userSessionId)
                .status("ACTIVE")
                .createdAt(System.currentTimeMillis())
                .expiresAt(System.currentTimeMillis() + ttlSeconds * 1000)
                .build();

        refreshTokenRepository.save(refreshToken);
        return token;
    }

    @Override
    public AuthRefreshToken getRefreshToken(String token) {
        List<AuthRefreshToken> tokens = refreshTokenRepository.findByToken(token);
        if (tokens.isEmpty()) {
            return null;
        }
        AuthRefreshToken refreshToken = tokens.get(0);
        if (!"ACTIVE".equals(refreshToken.getStatus()) || refreshToken.getExpiresAt() < System.currentTimeMillis()) {
            return null;
        }
        return refreshToken;
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String token) {
        List<AuthRefreshToken> tokens = refreshTokenRepository.findByToken(token);
        if (tokens.isEmpty()) {
            return;
        }
        AuthRefreshToken refreshToken = tokens.get(0);
        refreshToken.setStatus("REVOKED");
        refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public void revokeBySession(String sessionId, String clientId) {
        refreshTokenRepository.revokeBySessionIdAndClientId(sessionId, clientId);
        log.info("Revoked refresh tokens: sessionId={}, clientId={}", sessionId, clientId);
    }

    @Override
    @Transactional
    public void revokeAllByUserId(Long userId) {
        int count = refreshTokenRepository.revokeAllByUserId(userId);
        log.info("Revoked {} refresh tokens for userId={}", count, userId);
    }

    @Override
    @Transactional
    public void revokeByUserIdAndAppId(Long userId, Long appId) {
        int count = refreshTokenRepository.revokeByUserIdAndAppId(userId, appId);
        log.info("Revoked {} refresh tokens for userId={}, appId={}", count, userId, appId);
    }

    @Override
    @Transactional
    public void revokeAllByClientId(String clientId) {
        int count = refreshTokenRepository.revokeAllByClientId(clientId);
        log.info("Revoked {} refresh tokens for clientId={}", count, clientId);
    }
}
