package com.iam.identity.repository.jpa;

import com.iam.identity.domain.AuthUserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthUserSessionRepository extends JpaRepository<AuthUserSession, String> {

    List<AuthUserSession> findByUserId(Long userId);

    List<AuthUserSession> findByUserIdAndStatus(Long userId, Integer status);
}
