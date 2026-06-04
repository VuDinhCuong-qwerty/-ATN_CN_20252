package com.iam.auth.service;

import com.iam.auth.engine.AuthSession;

import java.util.Optional;

public interface AuthSessionService {
    void save(AuthSession session);

    Optional<AuthSession> getSessionById(String sessionId);

    void delete(String sessionId);
}
