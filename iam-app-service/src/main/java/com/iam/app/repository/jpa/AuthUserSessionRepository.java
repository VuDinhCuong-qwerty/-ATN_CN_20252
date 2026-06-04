package com.iam.app.repository.jpa;

import com.iam.app.domain.AuthUserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthUserSessionRepository extends JpaRepository<AuthUserSession, String> {
}
