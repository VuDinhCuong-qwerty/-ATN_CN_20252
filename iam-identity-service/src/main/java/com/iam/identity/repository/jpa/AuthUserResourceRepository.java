package com.iam.identity.repository.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.iam.identity.domain.AuthUserResource;

@Repository
public interface AuthUserResourceRepository extends JpaRepository<AuthUserResource, Long> {

    @Query(value = "SELECT t.RESOURCE_ID FROM AUTH_USER_RESOURCE t WHERE t.USER_ID = :userId AND t.STATUS = 'ACTIVE'", nativeQuery = true)
    List<Long> findResourcesByUserId(Long userId);

    @Modifying
    @Query(value = "UPDATE AUTH_USER_RESOURCE SET STATUS = 'SUSPENDED', INACTIVE_FROM_DATE = :fromDate, INACTIVE_TO_DATE = :toDate WHERE USER_ID = :userId AND STATUS = 'ACTIVE'", nativeQuery = true)
    void suspendWithDates(@Param("userId") Long userId,
                          @Param("fromDate") LocalDateTime fromDate,
                          @Param("toDate") LocalDateTime toDate);

    @Modifying
    @Query(value = "UPDATE AUTH_USER_RESOURCE SET INACTIVE_TO_DATE = :toDate WHERE USER_ID = :userId AND STATUS = 'SUSPENDED'", nativeQuery = true)
    void extendInactiveToDate(@Param("userId") Long userId,
                              @Param("toDate") LocalDateTime toDate);

    @Modifying
    @Query(value = "UPDATE AUTH_USER_RESOURCE SET STATUS = 'ACTIVE', INACTIVE_FROM_DATE = NULL, INACTIVE_TO_DATE = NULL WHERE USER_ID = :userId AND STATUS = 'SUSPENDED'", nativeQuery = true)
    void restoreAndClearDates(@Param("userId") Long userId);

    @Modifying
    @Query(value = "UPDATE AUTH_USER_RESOURCE SET STATUS = 'REVOKED', REVOKED_AT = :revokedAt WHERE USER_ID = :userId AND STATUS = 'ACTIVE'", nativeQuery = true)
    void revokeAllActiveByUserId(@Param("userId") Long userId, @Param("revokedAt") LocalDateTime revokedAt);

    @Modifying
    @Query(value = "UPDATE AUTH_USER_RESOURCE SET STATUS = 'REVOKED', REVOKED_AT = :revokedAt WHERE USER_ID = :userId AND STATUS IN ('ACTIVE', 'SUSPENDED')", nativeQuery = true)
    void revokeAllByUserId(@Param("userId") Long userId, @Param("revokedAt") LocalDateTime revokedAt);

    @Query(value = "SELECT COUNT(*) FROM AUTH_USER_RESOURCE WHERE USER_ID = :userId AND RESOURCE_ID = :resourceId AND STATUS = 'ACTIVE'", nativeQuery = true)
    int countActiveByUserIdAndResourceId(@Param("userId") Long userId, @Param("resourceId") Long resourceId);

    @Query(value = "SELECT COUNT(*) FROM AUTH_USER_RESOURCE WHERE USER_ID = :userId AND RESOURCE_ID = :resourceId AND ACTION = :action AND STATUS = 'ACTIVE'", nativeQuery = true)
    int countActiveByUserIdAndResourceIdAndAction(@Param("userId") Long userId,
                                                  @Param("resourceId") Long resourceId,
                                                  @Param("action") String action);

    @Query(value = "SELECT * FROM AUTH_USER_RESOURCE WHERE USER_ID = :userId AND RESOURCE_ID = :resourceId AND STATUS = 'ACTIVE' AND ROWNUM = 1", nativeQuery = true)
    Optional<AuthUserResource> findActiveByUserIdAndResourceId(@Param("userId") Long userId,
                                                               @Param("resourceId") Long resourceId);

    @Query(value = "SELECT * FROM AUTH_USER_RESOURCE WHERE USER_ID = :userId AND RESOURCE_ID = :resourceId AND ROWNUM = 1", nativeQuery = true)
    List<AuthUserResource> findByUserIdAndResourceId(@Param("userId") Long userId,
                                                         @Param("resourceId") Long resourceId);

    @Query(value = "SELECT * FROM AUTH_USER_RESOURCE WHERE USER_ID = :userId AND RESOURCE_ID IN (:resourceIds) AND STATUS = 'ACTIVE'", nativeQuery = true)
    List<AuthUserResource> findActiveByUserIdAndResourceIds(@Param("userId") Long userId,
                                                            @Param("resourceIds") Set<Long> resourceIds);

    @Query(value = "SELECT * FROM AUTH_USER_RESOURCE WHERE USER_ID = :userId AND RESOURCE_ID IN (:resourceIds)", nativeQuery = true)
    List<AuthUserResource> findAnyByUserIdAndResourceIdIn(@Param("userId") Long userId,
                                                          @Param("resourceIds") Set<Long> resourceIds);

    @Query(value = "SELECT * FROM AUTH_USER_RESOURCE r WHERE r.USER_ID = :userId AND r.RESOURCE_ID IN (SELECT ID FROM AUTH_RESOURCE WHERE APP_ID IN (:appIds)) AND r.STATUS = 'ACTIVE'", nativeQuery = true)
    List<AuthUserResource> findActiveByUserIdAndAppIdIn(@Param("userId") Long userId, @Param("appIds") Set<Long> appIds);

    @Query(value = "SELECT * FROM AUTH_USER_RESOURCE WHERE USER_ID = :userId AND STATUS = :status",
           countQuery = "SELECT COUNT(*) FROM AUTH_USER_RESOURCE WHERE USER_ID = :userId AND STATUS = :status",
           nativeQuery = true)
    Page<AuthUserResource> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status, Pageable pageable);
}
