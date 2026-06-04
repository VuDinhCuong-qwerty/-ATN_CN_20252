package com.iam.auth.service;

import com.iam.auth.domain.AuthUserSession;
import com.iam.auth.engine.AuthSession;

public interface SSOService {
    String createSSOSession(AuthSession session);

    String attachOrCreateSSOSession(AuthSession session, String existingSsoSessionId);

    AuthUserSession verifyUserSession(String ssoSession, Long clientId);

    void revokeAllSessionsByUserId(Long userId);

    void revokeSession(String sessionId);
}
