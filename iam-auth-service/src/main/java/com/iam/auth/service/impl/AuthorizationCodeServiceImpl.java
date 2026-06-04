package com.iam.auth.service.impl;

import com.iam.auth.domain.AuthUserSession;
import com.iam.auth.dto.pojo.AuthCode;
import com.iam.auth.dto.pojo.Client;
import com.iam.auth.dto.request.AuthorizeRequest;
import com.iam.auth.service.AuthorizationCodeService;
import com.iam.auth.service.BaseService;
import com.iam.auth.utils.Utility;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthorizationCodeServiceImpl extends BaseService implements AuthorizationCodeService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final String KEY_PREFIX = "auth:code:";
    private final String TOKEN_CODE_INDEX_PREFIX = "auth:token:code:";
    private final String USED_KEY_PREFIX = "auth:code:used";
    private final Long TTL_SEC = 60L;

    @Override
    public String createAuthorizationCode(AuthorizeRequest request, Client client, AuthUserSession session) {
        String code = Utility.generateAuthorizationCode();
        AuthCode authCode = AuthCode.builder()
                .clientId(client.getClientId())
                .redirectUri(request.getRedirectUri())
                .userId(session.getUserId())
                .userSessionId(session.getId())
                .scopes(request.getScopes())
                .codeChallenge(request.getCodeChallenge())
                .codeChallengeMethod(request.getCodeChallengeMethod())
                .nonce(request.getNonce())
                .issuerAt(LocalDateTime.now())
                .build();
        redisTemplate.opsForValue().set(KEY_PREFIX + code, authCode, TTL_SEC, TimeUnit.SECONDS);
        return code;
    }

    @Override
    public AuthCode getAuthorizationCode(String code) {
        if (code == null || code.isBlank()) return null;
        Object value = redisTemplate.opsForValue().get(KEY_PREFIX + code);
        if (value == null) return null;
        return (AuthCode) value;
    }


    @Override
    public boolean isUsed(String code) {
        return this.redisTemplate.hasKey(USED_KEY_PREFIX + code);
    }

    @Override
    public boolean markCodeUsed(String code) {
        Long remainingTTL = redisTemplate.getExpire(KEY_PREFIX + code, TimeUnit.SECONDS);
        long ttl = remainingTTL > 0 ? remainingTTL : TTL_SEC;

        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(USED_KEY_PREFIX + code, "1", ttl, TimeUnit.SECONDS)
        );
    }

    @Override
    public void revokeTokensByCode(String code) {
        String indexKey = TOKEN_CODE_INDEX_PREFIX + code;
        Set<Object> tokenKeys = redisTemplate.opsForSet().members(indexKey);
        if (tokenKeys != null && !tokenKeys.isEmpty()) {
            tokenKeys.forEach(key -> redisTemplate.delete(key.toString()));
        }
        redisTemplate.delete(indexKey);
        log.warn("Revoked all tokens for auth code: {}", code);
    }

}
