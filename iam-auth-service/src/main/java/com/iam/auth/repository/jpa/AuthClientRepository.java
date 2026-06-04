package com.iam.auth.repository.jpa;

import com.iam.auth.domain.AuthClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthClientRepository extends JpaRepository<AuthClient, Long> {
    @Query(value = "SELECT * FROM AUTH_CLIENT a WHERE a.APP_ID = :appId", nativeQuery = true)
    List<AuthClient> getClientByAppId(@Param("appId") Long groupId);

    @Query(value = "SELECT * FROM AUTH_CLIENT a WHERE a.ID = :Id", nativeQuery = true)
    List<AuthClient> getClientById(@Param("Id") Long id);

    @Query(value = "SELECT * FROM AUTH_CLIENT a WHERE a.CLIENT_ID = :clientId", nativeQuery = true)
    List<AuthClient> getClientByClientId(@Param("clientId") String clientId);
}
