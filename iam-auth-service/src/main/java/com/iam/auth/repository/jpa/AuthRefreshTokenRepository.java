package com.iam.auth.repository.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.iam.auth.domain.AuthRefreshToken;

@Repository
public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, String> {

    @Query(value = "SELECT * FROM AUTH_REFRESH_TOKEN t WHERE t.TOKEN = :token", nativeQuery = true)
    List<AuthRefreshToken> findByToken(String token);

    @Modifying
    @Transactional
    @Query(value = "UPDATE AUTH_REFRESH_TOKEN t SET t.STATUS = 'REVOKED' " +
            "WHERE t.USER_SESSION_ID = :sessionId AND t.CLIENT_ID = :clientId AND t.STATUS = 'ACTIVE'",
            nativeQuery = true)
    void revokeBySessionIdAndClientId(@Param("sessionId") String sessionId,
                                      @Param("clientId") String clientId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE AUTH_REFRESH_TOKEN SET STATUS = 'REVOKED' WHERE USER_ID = :userId AND STATUS = 'ACTIVE'",
            nativeQuery = true)
    int revokeAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE AUTH_REFRESH_TOKEN SET STATUS = 'REVOKED' WHERE USER_ID = :userId AND APP_ID = :appId AND STATUS = 'ACTIVE'",
            nativeQuery = true)
    int revokeByUserIdAndAppId(@Param("userId") Long userId, @Param("appId") Long appId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE AUTH_REFRESH_TOKEN SET STATUS = 'REVOKED' WHERE CLIENT_ID = :clientId AND STATUS = 'ACTIVE'",
            nativeQuery = true)
    int revokeAllByClientId(@Param("clientId") String clientId);
}
