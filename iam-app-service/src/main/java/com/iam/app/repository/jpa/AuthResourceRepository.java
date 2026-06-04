package com.iam.app.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iam.app.domain.AuthResource;

@Repository
public interface AuthResourceRepository extends JpaRepository<AuthResource, Long> {

    @Query(value = "SELECT * FROM AUTH_RESOURCE WHERE ID = :id", nativeQuery = true)
    Optional<AuthResource> findById(@Param("id") Long id);

    @Query(value = "SELECT RESOURCE_CODE FROM AUTH_RESOURCE WHERE APP_ID = :appId AND RESOURCE_CODE IN :codes", nativeQuery = true)
    List<String> findExistingResourceCodes(@Param("appId") Long appId, @Param("codes") List<String> codes);

    @Query(value = "SELECT * FROM AUTH_RESOURCE WHERE APP_ID = :appId AND STATUS = 'ACTIVE'", nativeQuery = true)
    List<AuthResource> findActiveByAppId(@Param("appId") Long appId);

    @Query(
        value = "SELECT * FROM AUTH_RESOURCE " +
                "WHERE APP_ID = :appId " +
                "AND (:status IS NULL OR STATUS = :status) " +
                "AND (:type IS NULL OR RESOURCE_TYPE = :type) " +
                "AND (:name IS NULL OR LOWER(RESOURCE_NAME) LIKE LOWER(:name)) " +
                "AND (:resourceCode IS NULL OR RESOURCE_CODE = :resourceCode) " +
                "ORDER BY CREATED_AT DESC",
        countQuery = "SELECT COUNT(*) FROM AUTH_RESOURCE " +
                     "WHERE APP_ID = :appId " +
                     "AND (:status IS NULL OR STATUS = :status) " +
                     "AND (:type IS NULL OR RESOURCE_TYPE = :type) " +
                     "AND (:name IS NULL OR LOWER(RESOURCE_NAME) LIKE LOWER(:name)) " +
                     "AND (:resourceCode IS NULL OR RESOURCE_CODE = :resourceCode)",
        nativeQuery = true
    )
    Page<AuthResource> findByFilters(
            @Param("appId") Long appId,
            @Param("status") String status,
            @Param("type") String type,
            @Param("name") String name,
            @Param("resourceCode") String resourceCode,
            Pageable pageable);
}
