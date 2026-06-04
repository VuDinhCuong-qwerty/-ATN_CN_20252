package com.iam.identity.repository.jpa;

import com.iam.identity.domain.AuthClientSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthClientSessionRepository extends JpaRepository<AuthClientSession, Long> {

    List<AuthClientSession> findBySessionId(String sessionId);

    List<AuthClientSession> findBySessionIdAndAppId(String sessionId, Long appId);
}
