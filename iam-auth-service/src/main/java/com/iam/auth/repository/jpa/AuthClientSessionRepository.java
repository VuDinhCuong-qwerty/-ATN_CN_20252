package com.iam.auth.repository.jpa;

import com.iam.auth.domain.AuthClientSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface AuthClientSessionRepository extends JpaRepository<AuthClientSession, Long> {

    @Query(value = "SELECT DISTINCT(app.GROUP_ID) " +
            "FROM AUTH_CLIENT_SESSION cs " +
            "JOIN AUTH_APPLICATION app ON cs.APP_ID = app.ID " +
            "WHERE cs.USER_SESSION_ID = :sessionId " +
            "AND cs.STATUS = 1 " +
            "AND cs.CREATED_AT <= :sysdate " +
            "AND cs.EXPIRES_AT >= :sysdate", nativeQuery = true)
    Set<Long> getGroupIdBySessionId(@Param("sessionId") String sessionId, @Param("sysdate") LocalDateTime sysdate);

    @Modifying
    @Transactional
    @Query(value = "UPDATE AUTH_CLIENT_SESSION cs " +
            "SET cs.EXPIRES_AT = :sysdate " +
            "WHERE cs.ID = :id", nativeQuery = true)
    void updateExpiresAt(@Param("id") Long id, @Param("sysdate") LocalDateTime sysdate);

    @Modifying
    @Transactional
    @Query(value = "UPDATE AUTH_CLIENT_SESSION cs " +
            "SET cs.STATUS = 0 " +
            "WHERE cs.USER_SESSION_ID = :sessionId AND cs.APP_ID = :appId AND cs.STATUS = 1",
            nativeQuery = true)
    void invalidateBySessionIdAndAppId(@Param("sessionId") String sessionId, @Param("appId") Long appId);

    @Query(value = "SELECT * FROM AUTH_CLIENT_SESSION s WHERE s.USER_SESSION_ID = :sessionId AND s.APP_ID = :appId", nativeQuery = true)
    List<AuthClientSession> findBySessionIdAndAppId(@Param("sessionId") String sessionId, @Param("appId") Long appId);
}
