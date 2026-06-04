package com.iam.auth.repository.jpa;

import com.iam.auth.domain.AuthUserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuthUserSessionRepository extends JpaRepository<AuthUserSession, String> {

    @Query(value = "SELECT * FROM AUTH_USER_SESSION s WHERE s.ID = :sessionId", nativeQuery = true)
    List<AuthUserSession> getSessionById(@Param("sessionId") String sessionId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE AUTH_USER_SESSION s " +
            "SET s.LAST_ACCESS = :lastAccess, s.EXPIRES_AT = :expiredAt " +
            "WHERE s.ID = :sessionId", nativeQuery = true)
    void updateLastAccess(@Param("sessionId") String sessionId,
                          @Param("lastAccess") LocalDateTime lastAccess,
                          @Param("expiredAt") LocalDateTime expiredAt
    );

    @Query(value = "SELECT * FROM AUTH_USER_SESSION s WHERE s.USER_ID = :userId AND s.STATUS = 1", nativeQuery = true)
    List<AuthUserSession> findActiveByUserId(@Param("userId") Long userId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE AUTH_USER_SESSION SET STATUS = 0 WHERE USER_ID = :userId AND STATUS = 1", nativeQuery = true)
    int revokeAllByUserId(@Param("userId") Long userId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE AUTH_USER_SESSION SET STATUS = 0 WHERE ID = :sessionId AND STATUS = 1", nativeQuery = true)
    int revokeById(@Param("sessionId") String sessionId);
}
