package com.iam.auth.service.impl;

import com.iam.auth.engine.AuthSession;
import com.iam.auth.service.AuthSessionService;
import com.iam.auth.service.BaseService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class AuthSessionServiceImpl extends BaseService implements AuthSessionService {

    private static final String KEY_PREFIX = "auth:session:";
    private static final long TTL_SESSION = 5;
    private final RedisTemplate<String, Object> redisTemplate;

    public AuthSessionServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String buildKey(String sessionId) {
        return KEY_PREFIX + sessionId;
    }

    @Override
    public void save(AuthSession session) {
        String cacheKey = this.buildKey(session.getSessionId());
        redisTemplate.opsForValue().set(cacheKey, session, Duration.ofMinutes(TTL_SESSION));
    }

    @Override
    public Optional<AuthSession> getSessionById(String sessionId) {
        String key = buildKey(sessionId);
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) return Optional.empty();
        return Optional.of((AuthSession) value);
    }

    @Override
    public void delete(String sessionId) {
        redisTemplate.delete(buildKey(sessionId));
    }
}
