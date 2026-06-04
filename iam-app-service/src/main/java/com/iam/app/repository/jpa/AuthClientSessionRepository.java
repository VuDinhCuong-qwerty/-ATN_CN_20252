package com.iam.app.repository.jpa;

import com.iam.app.domain.AuthClientSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthClientSessionRepository extends JpaRepository<AuthClientSession, Long> {
}
