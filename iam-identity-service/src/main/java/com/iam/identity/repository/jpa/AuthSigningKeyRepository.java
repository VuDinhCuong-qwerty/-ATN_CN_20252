package com.iam.identity.repository.jpa;

import com.iam.identity.domain.AuthSigningKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthSigningKeyRepository extends JpaRepository<AuthSigningKey, Long> {

    Optional<AuthSigningKey> findByKid(String kid);

    List<AuthSigningKey> findByStatus(String status);

    Optional<AuthSigningKey> findFirstByStatusOrderByCreatedAtDesc(String status);
}
