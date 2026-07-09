package com.iam.app.repository.jpa;

import com.iam.app.domain.AuthDefaultResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthDefaultResourceRepository extends JpaRepository<AuthDefaultResource, Long> {

    @Query(value = "SELECT * FROM AUTH_DEFAULT_RESOURCE WHERE RESOURCE_ID = :resourceId", nativeQuery = true)
    List<AuthDefaultResource> findByResourceId(@Param("resourceId") Long resourceId);

    @Query(
        value = "SELECT dr.* FROM AUTH_DEFAULT_RESOURCE dr " +
                "JOIN AUTH_RESOURCE r ON r.ID = dr.RESOURCE_ID " +
                "WHERE (:roleId IS NULL OR dr.ROLE_ID = :roleId) " +
                "AND (:positionCode IS NULL OR dr.POSITION_CODE = :positionCode) " +
                "AND (:resourceId IS NULL OR dr.RESOURCE_ID = :resourceId) " +
                "AND (:status IS NULL OR dr.STATUS = :status) " +
                "AND (:applicationId IS NULL OR r.APP_ID = :applicationId) " +
                "ORDER BY dr.CREATED_AT DESC",
        countQuery = "SELECT COUNT(*) FROM AUTH_DEFAULT_RESOURCE dr " +
                     "JOIN AUTH_RESOURCE r ON r.ID = dr.RESOURCE_ID " +
                     "WHERE (:roleId IS NULL OR dr.ROLE_ID = :roleId) " +
                     "AND (:positionCode IS NULL OR dr.POSITION_CODE = :positionCode) " +
                     "AND (:resourceId IS NULL OR dr.RESOURCE_ID = :resourceId) " +
                     "AND (:status IS NULL OR dr.STATUS = :status) " +
                     "AND (:applicationId IS NULL OR r.APP_ID = :applicationId)",
        nativeQuery = true
    )
    Page<AuthDefaultResource> findByFilters(
            @Param("roleId") Long roleId,
            @Param("positionCode") String positionCode,
            @Param("resourceId") Long resourceId,
            @Param("status") String status,
            @Param("applicationId") Long applicationId,
            Pageable pageable);

    @Query(value = "SELECT * FROM AUTH_DEFAULT_RESOURCE WHERE ROLE_ID IN :roleIds", nativeQuery = true)
    List<AuthDefaultResource> findByRoleIdIn(@Param("roleIds") List<Long> roleIds);

    @Query(
        value = "SELECT * FROM AUTH_DEFAULT_RESOURCE " +
                "WHERE (:roleId IS NULL OR ROLE_ID = :roleId) " +
                "AND (:positionCode IS NULL OR POSITION_CODE = :positionCode) " +
                "AND STATUS = 'ACTIVE'",
        nativeQuery = true
    )
    List<AuthDefaultResource> findActiveByRoleAndPosition(
            @Param("roleId") Long roleId,
            @Param("positionCode") String positionCode);

    @Query(value = "SELECT COUNT(*) FROM AUTH_DEFAULT_RESOURCE dr " +
                    "JOIN AUTH_RESOURCE r ON r.ID = dr.RESOURCE_ID " +
                    "WHERE r.APP_ID = :applicationId AND dr.STATUS = 'ACTIVE'", nativeQuery = true)
    int countActiveByApplicationId(@Param("applicationId") Long applicationId);
}
