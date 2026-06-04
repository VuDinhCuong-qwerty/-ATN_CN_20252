package com.iam.identity.repository.jpa;

import com.iam.identity.domain.AuthUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface AuthUserRoleRepository extends JpaRepository<AuthUserRole, Long> {

    List<AuthUserRole> findByUserId(Long userId);

    List<AuthUserRole> findByUserIdAndStatus(Long userId, String status);

    Optional<AuthUserRole> findByUserIdAndRoleId(Long userId, Long roleId);

    boolean existsByUserIdAndRoleIdAndStatus(Long userId, Long roleId, String status);

    @Modifying
    @Query(value = "UPDATE AUTH_USER_ROLE SET STATUS = 'REVOKED' WHERE USER_ID = :userId AND STATUS = 'ACTIVE'", nativeQuery = true)
    void revokeAllActiveByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT COUNT(*) FROM AUTH_USER_ROLE ur JOIN AUTH_ROLE r ON ur.ROLE_ID = r.ID WHERE ur.USER_ID = :userId AND r.CODE = :roleCode AND ur.STATUS = 'ACTIVE'", nativeQuery = true)
    int countActiveByUserIdAndRoleCode(@Param("userId") Long userId, @Param("roleCode") String roleCode);

    @Query(
        value = "SELECT * FROM AUTH_USER_ROLE WHERE USER_ID = :userId AND STATUS = :status",
        countQuery = "SELECT COUNT(*) FROM AUTH_USER_ROLE WHERE USER_ID = :userId AND STATUS = :status",
        nativeQuery = true
    )
    Page<AuthUserRole> findPagedByUserIdAndStatus(
        @Param("userId") Long userId,
        @Param("status") String status,
        Pageable pageable
    );
}
