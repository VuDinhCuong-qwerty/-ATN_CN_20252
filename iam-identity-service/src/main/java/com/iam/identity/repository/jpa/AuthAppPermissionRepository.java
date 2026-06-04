package com.iam.identity.repository.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.iam.identity.domain.AuthAppPermission;

@Repository
public interface AuthAppPermissionRepository extends JpaRepository<AuthAppPermission, Long> {

    @Query(value = "SELECT app.APP_ID FROM AUTH_APP_PERMISSION app WHERE app.USER_ID = :userId AND app.STATUS IN ('ACTIVE', 'SUSPENDED')", nativeQuery = true)
    List<Long> findAppIdByUserId(Long userId);

    @Query(value = "SELECT * FROM AUTH_APP_PERMISSION WHERE USER_ID = :userId AND APP_ID = :appId", nativeQuery = true)
    List<AuthAppPermission> findByUserIdAndAppId(@Param("userId") Long userId, @Param("appId") Long appId);

    @Query(value = "SELECT * FROM AUTH_APP_PERMISSION WHERE USER_ID = :userId AND APP_ID IN (:appIds)", nativeQuery = true)
    List<AuthAppPermission> findAnyByUserIdAndAppIdIn(@Param("userId") Long userId, @Param("appIds") Set<Long> appIds);

    @Query(value = "SELECT * FROM AUTH_APP_PERMISSION WHERE USER_ID = :userId", nativeQuery = true)
    List<AuthAppPermission> findAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query(value = "UPDATE AUTH_APP_PERMISSION SET STATUS = 'SUSPENDED', INACTIVE_FROM_DATE = :fromDate, INACTIVE_TO_DATE = :toDate WHERE USER_ID = :userId AND STATUS = 'ACTIVE'", nativeQuery = true)
    void suspendWithDates(@Param("userId") Long userId,
                          @Param("fromDate") LocalDateTime fromDate,
                          @Param("toDate") LocalDateTime toDate);

    @Modifying
    @Query(value = "UPDATE AUTH_APP_PERMISSION SET INACTIVE_TO_DATE = :toDate WHERE USER_ID = :userId AND STATUS = 'SUSPENDED'", nativeQuery = true)
    void extendInactiveToDate(@Param("userId") Long userId,
                              @Param("toDate") LocalDateTime toDate);

    @Modifying
    @Query(value = "UPDATE AUTH_APP_PERMISSION SET STATUS = 'ACTIVE', INACTIVE_FROM_DATE = NULL, INACTIVE_TO_DATE = NULL WHERE USER_ID = :userId AND STATUS = 'SUSPENDED'", nativeQuery = true)
    void restoreAndClearDates(@Param("userId") Long userId);

    @Modifying
    @Query(value = "UPDATE AUTH_APP_PERMISSION SET STATUS = 'REVOKED', REVOKED_AT = :revokedAt WHERE USER_ID = :userId AND STATUS = 'ACTIVE'", nativeQuery = true)
    void revokeAllActiveByUserId(@Param("userId") Long userId, @Param("revokedAt") LocalDateTime revokedAt);

    @Modifying
    @Query(value = "UPDATE AUTH_APP_PERMISSION SET STATUS = 'REVOKED', REVOKED_AT = :revokedAt WHERE USER_ID = :userId AND STATUS IN ('ACTIVE', 'SUSPENDED')", nativeQuery = true)
    void revokeAllByUserId(@Param("userId") Long userId, @Param("revokedAt") LocalDateTime revokedAt);

    @Query(value = "SELECT * FROM AUTH_APP_PERMISSION WHERE USER_ID = :userId AND APP_ID IN (:appIds) AND STATUS = 'ACTIVE'", nativeQuery = true)
    List<AuthAppPermission> findAllActiveByUserIdAndAppIdIn(@Param("userId") Long userId, @Param("appIds") Set<Long> appIds);

    @Query(value = "SELECT * FROM AUTH_APP_PERMISSION WHERE USER_ID = :userId AND STATUS = :status",
           countQuery = "SELECT COUNT(*) FROM AUTH_APP_PERMISSION WHERE USER_ID = :userId AND STATUS = :status",
           nativeQuery = true)
    Page<AuthAppPermission> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status, Pageable pageable);
}
