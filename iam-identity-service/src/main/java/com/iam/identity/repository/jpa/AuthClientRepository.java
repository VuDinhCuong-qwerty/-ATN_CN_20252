package com.iam.identity.repository.jpa;

import com.iam.identity.domain.AuthClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthClientRepository extends JpaRepository<AuthClient, Long> {

    Optional<AuthClient> findByClientId(String clientId);

    List<AuthClient> findByAppId(Long appId);

    boolean existsByClientId(String clientId);
}
