package com.iam.auth.service.impl;

import com.iam.auth.domain.AuthApplication;
import com.iam.auth.domain.AuthClientSession;
import com.iam.auth.domain.AuthUserSession;
import com.iam.auth.engine.AuthSession;
import com.iam.auth.repository.jpa.*;
import com.iam.auth.service.BaseService;
import com.iam.auth.service.SSOService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SSOServiceImpl extends BaseService implements SSOService {

    private static final long SESSION_TTL_HOURS = 24L;
    private static final long ACR1_CLIENT_SESSION_TTL_MINUTES = 10L;

    private final AuthUserSessionRepository userSessionRepository;
    private final AuthClientSessionRepository clientSessionRepository;
    private final AuthApplicationRepository applicationRepository;

    @Override
    @Transactional
    public String createSSOSession(AuthSession session) {
        String sessionId = generateSessionID();
        LocalDateTime now = LocalDateTime.now();

        AuthUserSession ssoSession = AuthUserSession.builder()
                .id(sessionId).userId(session.getUserId()).status(1)
                .acrLevel(session.getArcLevel())
                .createdAt(now).lastAccess(now).expiresAt(now.plusHours(SESSION_TTL_HOURS))
                .ipAddress(null).userAgent(null)
                .build();
        this.userSessionRepository.save(ssoSession);
        log.info("SSO session created: sessionId={}, userId={}", sessionId, session.getUserId());

        this.clientSessionRepository
                .save(buildClientSession(sessionId, session.getAppId(), now, session.getArcLevel()));
        return sessionId;
    }

    @Override
    @Transactional
    public String attachOrCreateSSOSession(AuthSession session, String existingSsoSessionId) {
        LocalDateTime now = LocalDateTime.now();

        if (existingSsoSessionId != null && !existingSsoSessionId.isBlank()) {
            List<AuthUserSession> sessions = this.userSessionRepository.getSessionById(existingSsoSessionId);
            if (sessions != null && sessions.size() == 1) {
                AuthUserSession existing = sessions.getFirst();
                if (Integer.valueOf(1).equals(existing.getStatus())
                        && !now.isAfter(existing.getExpiresAt())
                        && !now.isBefore(existing.getCreatedAt())) {
                    this.clientSessionRepository.save(
                            buildClientSession(existingSsoSessionId, session.getAppId(), now, session.getArcLevel()));
                    log.info("SSO client session attached: sessionId={}, appId={}", existingSsoSessionId,
                            session.getAppId());
                    return existingSsoSessionId;
                }
            }
        }

        return createSSOSession(session);
    }

    @Override
    @Transactional
    public AuthUserSession verifyUserSession(String ssoSession, Long clientId) {
        LocalDateTime now = LocalDateTime.now();
        if (ssoSession == null || ssoSession.isBlank())
            return null;

        List<AuthUserSession> sessions = this.userSessionRepository.getSessionById(ssoSession);
        if (sessions == null || sessions.size() != 1)
            return null;

        AuthUserSession session = sessions.getFirst();

        if (!Integer.valueOf(1).equals(session.getStatus()))
            return null;

        if (now.isAfter(session.getExpiresAt()) || now.isBefore(session.getCreatedAt()))
            return null;
        List<AuthApplication> applications = this.applicationRepository.getAppByClientId(clientId, "ACTIVE");
        if (applications == null || applications.size() != 1)
            return null;
        AuthApplication authApplication = applications.getFirst();

        // acr=1 apps always require fresh login — no SSO bypass possible
        if (authApplication.getAcrLevel() == 1)
            return null;

        Set<Long> groupIds = this.clientSessionRepository.getGroupIdBySessionId(ssoSession, now);
        if (groupIds == null || groupIds.isEmpty() || !groupIds.contains(authApplication.getGroupId()))
            return null;

        session.setLastAccess(now);
        session.setExpiresAt(now.plusHours(SESSION_TTL_HOURS));
        this.userSessionRepository.updateLastAccess(ssoSession, session.getLastAccess(), session.getExpiresAt());

        return session;
    }

    private AuthClientSession buildClientSession(String sessionId, Long appId, LocalDateTime now, Long arcLevel) {
        List<AuthClientSession> existingSessions = this.clientSessionRepository.findBySessionIdAndAppId(sessionId,
                appId);
        if (existingSessions == null || existingSessions.isEmpty()) {
            long ttlMinutes = Long.valueOf(1L).equals(arcLevel) ? ACR1_CLIENT_SESSION_TTL_MINUTES
                    : SESSION_TTL_HOURS * 60;
            return AuthClientSession.builder()
                    .sessionId(sessionId).appId(appId)
                    .status(1L).createdAt(now).expiredAt(now.plusMinutes(ttlMinutes))
                    .build();
        } else {
            return existingSessions.getFirst();
        }
    }

    @Override
    @Transactional
    public void revokeAllSessionsByUserId(Long userId) {
        int count = userSessionRepository.revokeAllByUserId(userId);
        log.info("Revoked {} SSO sessions for userId={}", count, userId);
    }

    @Override
    @Transactional
    public void revokeSession(String sessionId) {
        int count = userSessionRepository.revokeById(sessionId);
        log.info("Revoked SSO session: sessionId={}, affected={}", sessionId, count);
    }

    private String generateSessionID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
