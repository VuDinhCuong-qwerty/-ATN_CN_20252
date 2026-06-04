package com.iam.app.repository.jpa;

import com.iam.app.domain.AuthClientOauth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthClientOauthRepository extends JpaRepository<AuthClientOauth, Long> {

    @Query(value = "SELECT * FROM AUTH_CLIENT_OAUTH WHERE CLIENT_ID = :clientId", nativeQuery = true)
    Optional<AuthClientOauth> findByClientId(@Param("clientId") Long clientId);
}
