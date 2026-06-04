package com.iam.app.repository.jpa;

import com.iam.app.domain.AuthDefaultAppPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthDefaultAppPermissionRepository extends JpaRepository<AuthDefaultAppPermission, Long> {

    @Query(
        value = "SELECT * FROM AUTH_DEFAULT_APP_PERMISSION " +
                "WHERE (:roleId IS NULL OR ROLE_ID = :roleId) " +
                "AND (:positionCode IS NULL OR POSITION_CODE = :positionCode) " +
                "AND (:applicationId IS NULL OR APPLICATION_ID = :applicationId) " +
                "AND (:status IS NULL OR STATUS = :status) " +
                "ORDER BY CREATED_AT DESC",
        countQuery = "SELECT COUNT(*) FROM AUTH_DEFAULT_APP_PERMISSION " +
                     "WHERE (:roleId IS NULL OR ROLE_ID = :roleId) " +
                     "AND (:positionCode IS NULL OR POSITION_CODE = :positionCode) " +
                     "AND (:applicationId IS NULL OR APPLICATION_ID = :applicationId) " +
                     "AND (:status IS NULL OR STATUS = :status)",
        nativeQuery = true
    )
    Page<AuthDefaultAppPermission> findByFilters(
            @Param("roleId") String roleId,
            @Param("positionCode") String positionCode,
            @Param("applicationId") Long applicationId,
            @Param("status") String status,
            Pageable pageable);

    @Query(
        value = "SELECT * FROM AUTH_DEFAULT_APP_PERMISSION WHERE ROLE_ID IN :roleIds",
        nativeQuery = true
    )
    List<AuthDefaultAppPermission> findByRoleIdIn(@Param("roleIds") List<String> roleIds);

    @Query(
        value = "SELECT * FROM AUTH_DEFAULT_APP_PERMISSION " +
                "WHERE (:roleId IS NULL OR ROLE_ID = :roleId) " +
                "AND (:positionCode IS NULL OR POSITION_CODE = :positionCode) " +
                "AND STATUS = 'ACTIVE'",
        nativeQuery = true
    )
    List<AuthDefaultAppPermission> findActiveByRoleAndPosition(
            @Param("roleId") String roleId,
            @Param("positionCode") String positionCode);
}
