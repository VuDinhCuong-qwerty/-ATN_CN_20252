package com.iam.app.repository.jpa;

import com.iam.app.domain.AuthClientMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthClientMethodRepository extends JpaRepository<AuthClientMethod, Long> {

    @Query(value = "SELECT * FROM AUTH_CLIENT_METHOD WHERE APP_ID = :appId ORDER BY CREATED_AT ASC", nativeQuery = true)
    List<AuthClientMethod> findByAppId(@Param("appId") Long appId);

    @Query(value = "SELECT COUNT(*) FROM AUTH_CLIENT_METHOD WHERE APP_ID = :appId AND METHOD_ID = :methodId", nativeQuery = true)
    int countByAppIdAndMethodId(@Param("appId") Long appId, @Param("methodId") Long methodId);

    @Query(value = "SELECT * FROM AUTH_CLIENT_METHOD WHERE APP_ID = :appId AND METHOD_ID = :methodId", nativeQuery = true)
    Optional<AuthClientMethod> findByAppIdAndMethodId(@Param("appId") Long appId, @Param("methodId") Long methodId);

    @Query(value = "SELECT * FROM AUTH_CLIENT_METHOD WHERE APP_ID = :appId AND METHOD_ID IN (:methodIds)", nativeQuery = true)
    List<AuthClientMethod> findAllByAppIdAndMethodIdIn(@Param("appId") Long appId, @Param("methodIds") List<Long> methodIds);
}
