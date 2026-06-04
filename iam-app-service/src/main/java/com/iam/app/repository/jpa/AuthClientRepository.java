package com.iam.app.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iam.app.domain.AuthClient;

@Repository
public interface AuthClientRepository extends JpaRepository<AuthClient, Long> {

    @Query(value = "SELECT * FROM AUTH_CLIENT WHERE APP_ID = :appId AND ENABLED = 1", nativeQuery = true)
    List<AuthClient> findEnabledByAppId(@Param("appId") Long appId);

    @Query(value = "SELECT COUNT(1) FROM AUTH_CLIENT WHERE CLIENT_ID = :clientId", nativeQuery = true)
    int countByClientId(@Param("clientId") String clientId);

    @Query(value = "SELECT * FROM AUTH_CLIENT WHERE CLIENT_ID = :clientId", nativeQuery = true)
    List<AuthClient> findByClientId(@Param("clientId") String clientId);

    @Query(value = "SELECT * FROM AUTH_CLIENT WHERE ID = :id", nativeQuery = true)
    Optional<AuthClient> findByNumericId(@Param("id") Long id);
}
