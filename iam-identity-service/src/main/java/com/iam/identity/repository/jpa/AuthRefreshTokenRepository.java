package com.iam.identity.repository.jpa;

import com.iam.identity.domain.AuthRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, String> {

    Optional<AuthRefreshToken> findByTokenAndStatus(String token, String status);

    List<AuthRefreshToken> findByUserIdAndStatus(Long userId, String status);

    List<AuthRefreshToken> findByUserSessionIdAndStatus(String userSessionId, String status);
}
