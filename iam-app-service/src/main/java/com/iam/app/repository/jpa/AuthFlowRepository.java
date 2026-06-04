package com.iam.app.repository.jpa;

import com.iam.app.domain.AuthFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthFlowRepository extends JpaRepository<AuthFlow, Long> {

    @Query(value = """
            SELECT * FROM AUTH_FLOW
            WHERE APP_ID = :appId
            AND (:status IS NULL OR STATUS = :status)
            ORDER BY CREATED_AT DESC
            """, nativeQuery = true)
    List<AuthFlow> findByAppId(@Param("appId") Long appId, @Param("status") String status);

    @Query(value = "SELECT * FROM AUTH_FLOW WHERE ID = :id AND APP_ID = :appId", nativeQuery = true)
    Optional<AuthFlow> findByIdAndAppId(@Param("id") Long id, @Param("appId") Long appId);

    @Query(value = "SELECT COUNT(*) FROM AUTH_FLOW WHERE APP_ID = :appId AND STATUS = 'ACTIVE'", nativeQuery = true)
    int countActiveByAppId(@Param("appId") Long appId);

    @Query(value = "SELECT COUNT(*) FROM AUTH_FLOW WHERE APP_ID = :appId AND ALIAS = :alias", nativeQuery = true)
    int countByAppIdAndAlias(@Param("appId") Long appId, @Param("alias") String alias);
}
