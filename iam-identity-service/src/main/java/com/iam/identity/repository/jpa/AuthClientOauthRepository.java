package com.iam.identity.repository.jpa;

import com.iam.identity.domain.AuthClientOauth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthClientOauthRepository extends JpaRepository<AuthClientOauth, Long> {

    Optional<AuthClientOauth> findByClientId(Long clientId);
}
